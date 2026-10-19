package sg.paralleye.sensors

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import sg.paralleye.domain.behaviour.ForegroundAppCategory
import sg.paralleye.session.PermissionChecker

/**
 * Ch.1 §12, Ch.6: gathers the one real-world Activity signal [sg.paralleye.domain.behaviour.
 * ActivityClassifier] was previously always fed as null -- which foreground-app category is
 * currently in use. Uses [UsageStatsManager], the Android-sanctioned API for exactly this (the
 * same one digital-wellbeing/screen-time apps use), rather than an `AccessibilityService`,
 * which Play Store review scrutinizes heavily for non-assistive use (see
 * docs/open-questions.md #18). Requires the user to grant Usage Access manually via Settings --
 * there is no runtime permission dialog for this, see [PermissionChecker.hasUsageAccessPermission].
 *
 * Keyboard-active detection (Ch.6's other, higher-priority signal) is NOT implemented here --
 * there is no non-accessibility-service API for "is some other app's keyboard open right now."
 * [sg.paralleye.domain.behaviour.ActivityClassifier] already degrades gracefully to
 * foreground-app-category when the keyboard signal is absent, so this is a real, substantial
 * improvement over the permanently-empty observation it replaces, not every Ch.6 signal.
 */
class ForegroundAppObserver(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    private var lastKnownPackage: String? = null
    private var currentPackageSinceMillis: Long? = null
    private var lastQueryAtMillis: Long = 0L
    private var cachedResult: Pair<ForegroundAppCategory, Int>? = null

    /**
     * @return the current foreground app's category, and how many whole seconds that same app
     * has continuously been foreground (an engineering proxy for "actively engaged" -- there is
     * no real touch/scroll signal available without a far more invasive permission; see
     * docs/open-questions.md #18) -- or null if Usage Access isn't granted, or no foreground
     * app could be determined yet.
     *
     * Deliberately uses its own internal [System.currentTimeMillis] clock, never a caller-
     * supplied timestamp: [UsageEvents] timestamps are wall-clock (epoch) based, unlike the
     * boot-relative [android.os.SystemClock.elapsedRealtime] the rest of the sensor pipeline
     * uses -- mixing the two here would silently misbehave, the same class of bug already found
     * and fixed once this session for the mascot tap/reappear timer.
     */
    fun observe(): Pair<ForegroundAppCategory, Int>? {
        if (!PermissionChecker.hasUsageAccessPermission(context)) return null

        val nowMillis = System.currentTimeMillis()
        if (nowMillis - lastQueryAtMillis < QUERY_THROTTLE_MILLIS && cachedResult != null) {
            return cachedResult
        }
        lastQueryAtMillis = nowMillis

        val packageName = currentForegroundPackage(nowMillis) ?: run {
            cachedResult = null
            return null
        }

        if (packageName != lastKnownPackage) {
            lastKnownPackage = packageName
            currentPackageSinceMillis = nowMillis
        }
        val continuousSeconds = ((nowMillis - (currentPackageSinceMillis ?: nowMillis)) / 1000).toInt()

        return (categoryFor(packageName) to continuousSeconds).also { cachedResult = it }
    }

    private fun currentForegroundPackage(nowMillis: Long): String? {
        // A lookback window wider than the throttle interval, so a scheduling gap in the
        // pipeline's own calling cadence can't make this miss the most recent foreground switch.
        val events = usageStatsManager.queryEvents(nowMillis - LOOKBACK_MILLIS, nowMillis)
        var latestForegroundPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isForegroundEvent) {
                latestForegroundPackage = event.packageName
            }
        }
        return latestForegroundPackage
    }

    /**
     * Ch.6 names category buckets (video/game/messaging-or-browser) but not how to derive them
     * from a package name -- this uses each app's own Play-declared [ApplicationInfo.category]
     * (the same metadata Play Console requires for store listing) rather than a hand-maintained
     * package list, so it doesn't silently misclassify every app this build's author didn't
     * think to list. An engineering mapping choice, not a filed methodology value.
     */
    private fun categoryFor(packageName: String): ForegroundAppCategory {
        val category = try {
            packageManager.getApplicationInfo(packageName, 0).category
        } catch (e: PackageManager.NameNotFoundException) {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
        return when (category) {
            ApplicationInfo.CATEGORY_GAME -> ForegroundAppCategory.GAME
            ApplicationInfo.CATEGORY_VIDEO -> ForegroundAppCategory.VIDEO_PLAYER
            ApplicationInfo.CATEGORY_SOCIAL -> ForegroundAppCategory.MESSAGING_OR_BROWSER
            else -> ForegroundAppCategory.OTHER
        }
    }

    companion object {
        private const val QUERY_THROTTLE_MILLIS = 2_000L
        private const val LOOKBACK_MILLIS = 60_000L
    }
}
