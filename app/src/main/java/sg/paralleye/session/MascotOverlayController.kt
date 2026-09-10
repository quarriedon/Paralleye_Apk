package sg.paralleye.session

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import sg.paralleye.config.MascotConfig
import sg.paralleye.domain.alert.AlertLevel
import sg.paralleye.domain.alert.MascotVisibility
import sg.paralleye.logging.ParallayeLogger
import sg.paralleye.ui.mascot.frameFor

/**
 * Ch.10 §12, §38: the mascot must be visible while PARALLEYE is backgrounded, not only while
 * [sg.paralleye.MainActivity] itself is on screen — that's what the app requests the
 * draw-over-other-apps permission for (`PermissionChecker.hasOverlayPermission`), which
 * previously gated monitoring startup without anything ever actually drawing an overlay
 * window with it.
 *
 * A plain [ImageView] via [WindowManager] rather than a `ComposeView` here — hosting Compose
 * outside an Activity needs its own manually-driven `LifecycleOwner`/`SavedStateRegistryOwner`
 * boilerplate, and this view's whole job is "show one of six static drawables, dismiss on
 * tap" — [sg.paralleye.ui.mascot.frameFor] already owns the level→drawable mapping, this class
 * only owns *how* to present it outside the app's own window.
 *
 * `update`/`hide` are called from [MonitoringForegroundService]'s `Dispatchers.Default`
 * coroutine scope, but [WindowManager.addView]/`removeView` require a thread with a prepared
 * `Looper` (the main thread) — confirmed the hard way via [OverlayDiagnosticLog]: every call
 * was throwing "Can't create handler inside thread ... that has not called Looper.prepare()",
 * silently swallowed by the `runCatching` below, which is the actual reason the background
 * overlay never appeared through several previous rounds of "fixing" this. Every mutation is
 * posted to [mainHandler] so this class is safe to drive from any thread.
 */
class MascotOverlayController(private val context: Context, private val config: MascotConfig) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: ImageView? = null
    private var currentLevel: AlertLevel? = null

    var onTapped: (() -> Unit)? = null

    fun update(visibility: MascotVisibility) {
        mainHandler.post {
            when (visibility) {
                MascotVisibility.Hidden -> hideOnMainThread()
                is MascotVisibility.Visible -> showOnMainThread(visibility.level)
            }
        }
    }

    fun hide() {
        mainHandler.post { hideOnMainThread() }
    }

    private fun showOnMainThread(level: AlertLevel) {
        val view = overlayView ?: runCatching { createView() }
            .onSuccess { OverlayDiagnosticLog.log("overlay addView OK, level=$level") }
            .onFailure {
                ParallayeLogger.error("MascotOverlayController", "addView failed", it)
                OverlayDiagnosticLog.log("overlay addView FAILED: ${it::class.simpleName} ${it.message}")
            }
            .getOrNull()?.also { overlayView = it }
            ?: return
        if (level != currentLevel) {
            view.setImageResource(frameFor(level))
            currentLevel = level
        }
        view.visibility = View.VISIBLE
    }

    private fun hideOnMainThread() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
            .onFailure { ParallayeLogger.error("MascotOverlayController", "removeView failed", it) }
        overlayView = null
        currentLevel = null
    }

    private fun createView(): ImageView {
        val density = context.resources.displayMetrics.density
        // Not a square: same reasoning as MsAngleAngelOverlay's KDoc -- the three cropped
        // character frames are portrait-oriented (~0.2-0.7 width:height depending on reveal
        // stage), and a fixed square box previously forced them down to fit, rendering far
        // smaller than intended. A real WindowManager window's size can't auto-follow each
        // frame's own aspect ratio the way Compose can without resizing the window on every
        // frame change, so this picks one fixed box close to the FULL_ALERT frame's aspect
        // ratio (the most-visible, most-important state), accepting letterboxing on the
        // narrower PEEK/PEEL frames instead. Matches MsAngleAngelOverlay's 260dp height per
        // client instruction ("not dim and small, up to 30% of that quadrant").
        val widthPx = (MASCOT_WIDTH_DP * density).toInt()
        val heightPx = (MASCOT_HEIGHT_DP * density).toInt()
        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Ch.10 §13/§43: tap dismisses immediately, so touches on the mascot itself must
            // still be delivered — NOT_FOCUSABLE keeps it from stealing keyboard/input focus
            // from whatever app is in front; NOT_TOUCH_MODAL lets touches outside its bounds
            // pass straight through to that app.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            // Right edge, not left -- see MsAngleAngelOverlay's KDoc for why this follows the
            // commissioned reference app over Ch.10 §38's written "upper-left" text.
            gravity = Gravity.TOP or Gravity.END
            x = (config.cornerOffsetXDp * density).toInt()
            y = (config.cornerOffsetYDp * density).toInt()
        }
        return ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { onTapped?.invoke() }
            contentDescription = "Ms Angle Angel posture reminder"
            windowManager.addView(this, params)
        }
    }

    companion object {
        private const val MASCOT_WIDTH_DP = 185
        private const val MASCOT_HEIGHT_DP = 260
    }
}
