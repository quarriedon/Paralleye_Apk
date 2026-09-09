package sg.paralleye.domain.behaviour

import org.junit.Assert.assertEquals
import org.junit.Test
import sg.paralleye.config.ActivityCategory

class ActivityClassifierTest {

    @Test
    fun `keyboard active classifies typing regardless of foreground app`() {
        val result = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = true, foregroundAppCategory = ForegroundAppCategory.GAME, continuousInteractionSeconds = 5),
        )
        assertEquals(ActivityCategory.TYPING, result.category)
        assertEquals(ClassificationConfidence.HIGH, result.confidence)
    }

    @Test
    fun `game foreground app classifies gaming`() {
        val result = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = false, foregroundAppCategory = ForegroundAppCategory.GAME, continuousInteractionSeconds = 5),
        )
        assertEquals(ActivityCategory.GAMING, result.category)
    }

    @Test
    fun `video player foreground app classifies video`() {
        val result = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = false, foregroundAppCategory = ForegroundAppCategory.VIDEO_PLAYER, continuousInteractionSeconds = 0),
        )
        assertEquals(ActivityCategory.VIDEO, result.category)
    }

    @Test
    fun `insufficient evidence falls back to unknown, never fabricated`() {
        val result = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = false, foregroundAppCategory = null, continuousInteractionSeconds = 0),
        )
        assertEquals(ActivityCategory.UNKNOWN, result.category)
    }

    @Test
    fun `unrecognised foreground category with no interaction falls back to unknown`() {
        val result = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = false, foregroundAppCategory = ForegroundAppCategory.OTHER, continuousInteractionSeconds = 0),
        )
        assertEquals(ActivityCategory.UNKNOWN, result.category)
    }
}
