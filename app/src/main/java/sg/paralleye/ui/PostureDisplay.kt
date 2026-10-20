package sg.paralleye.ui

import androidx.compose.ui.graphics.Color
import sg.paralleye.config.ActivityCategory
import sg.paralleye.domain.behaviour.PostureZone

/**
 * Shared display names/colours for [PostureZone] and [ActivityCategory], used by both the live
 * Monitor screen and the Progress screen so the two stay visually consistent. "Safe/Caution/
 * Alert" is UI-only wording for [PostureZone] -- the underlying GREEN/YELLOW/RED enum and every
 * zone-threshold/config name is untouched, same approach as the "posture assistance" copy pass
 * (user-facing text changed, internal naming didn't).
 */
object PostureDisplay {
    fun zoneLabel(zone: PostureZone): String = when (zone) {
        PostureZone.GREEN -> "Safe"
        PostureZone.YELLOW -> "Caution"
        PostureZone.RED -> "Alert"
    }

    fun zoneColor(zone: PostureZone): Color = when (zone) {
        PostureZone.GREEN -> Color(0xFF4CAF50)
        PostureZone.YELLOW -> Color(0xFFFF9800)
        PostureZone.RED -> Color(0xFFE91E63)
    }

    fun activityLabel(category: ActivityCategory): String = when (category) {
        ActivityCategory.VIDEO -> "Video Watching"
        ActivityCategory.SCROLLING -> "Scrolling"
        ActivityCategory.TYPING -> "Typing"
        ActivityCategory.GAMING -> "Gaming"
        ActivityCategory.UNKNOWN -> "Unknown"
    }

    fun activityColor(category: ActivityCategory): Color = when (category) {
        ActivityCategory.VIDEO -> Color(0xFF2196F3)
        ActivityCategory.SCROLLING -> Color(0xFF4CAF50)
        ActivityCategory.TYPING -> Color(0xFFFF9800)
        ActivityCategory.GAMING -> Color(0xFF9C27B0)
        ActivityCategory.UNKNOWN -> Color(0xFF9E9E9E)
    }
}
