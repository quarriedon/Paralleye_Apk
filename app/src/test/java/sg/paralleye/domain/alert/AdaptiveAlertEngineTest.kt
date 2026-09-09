package sg.paralleye.domain.alert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sg.paralleye.config.AlertStateRanges
import sg.paralleye.config.ParameterOrigin

class AlertLevelTest {

    private val ranges = AlertStateRanges(idleMin = 75, peekMin = 50, peelMin = 25, fullAlertMin = 0, origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION)

    @Test
    fun `boundaries classify per Ch10 Sec26,1`() {
        assertEquals(AlertLevel.IDLE, AlertLevel.classify(100, ranges))
        assertEquals(AlertLevel.IDLE, AlertLevel.classify(75, ranges))
        assertEquals(AlertLevel.PEEK, AlertLevel.classify(74, ranges))
        assertEquals(AlertLevel.PEEK, AlertLevel.classify(50, ranges))
        assertEquals(AlertLevel.PEEL, AlertLevel.classify(49, ranges))
        assertEquals(AlertLevel.PEEL, AlertLevel.classify(25, ranges))
        assertEquals(AlertLevel.FULL_ALERT, AlertLevel.classify(24, ranges))
        assertEquals(AlertLevel.FULL_ALERT, AlertLevel.classify(0, ranges))
    }
}

class AdaptiveAlertEngineTest {

    private val ranges = AlertStateRanges(idleMin = 75, peekMin = 50, peelMin = 25, fullAlertMin = 0, origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION)
    private val reappear = 60_000L
    private val promptWindow = 10_000L

    @Test
    fun `idle score keeps mascot hidden`() {
        val engine = AdaptiveAlertEngine()
        val result = engine.onCycle(90, ranges, nowMillis = 0, reappearanceIntervalMillis = reappear, promptCorrectionWindowMillis = promptWindow)
        assertEquals(MascotVisibility.Hidden, result.visibility)
    }

    @Test
    fun `poor score shows mascot at the current level`() {
        val engine = AdaptiveAlertEngine()
        val result = engine.onCycle(40, ranges, nowMillis = 0, reappearanceIntervalMillis = reappear, promptCorrectionWindowMillis = promptWindow)
        assertEquals(MascotVisibility.Visible(AlertLevel.PEEL), result.visibility)
    }

    @Test
    fun `dismissal hides mascot regardless of subsequent level changes until interval elapses`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow) // Full Alert
        engine.onMascotTapped(nowMillis = 0, reappearanceIntervalMillis = reappear)

        val stillHidden = engine.onCycle(5, ranges, nowMillis = 30_000, reappear, promptWindow)
        assertEquals(MascotVisibility.Hidden, stillHidden.visibility)

        val reappeared = engine.onCycle(5, ranges, nowMillis = 60_000, reappear, promptWindow)
        assertEquals(MascotVisibility.Visible(AlertLevel.FULL_ALERT), reappeared.visibility)
    }

    @Test
    fun `reappearance shows the current level, not the level at dismissal time`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow) // Full Alert
        engine.onMascotTapped(nowMillis = 0, reappearanceIntervalMillis = reappear)
        // posture partially improved while dismissed, but not to Idle
        val reappeared = engine.onCycle(60, ranges, nowMillis = 60_000, reappear, promptWindow)
        assertEquals(MascotVisibility.Visible(AlertLevel.PEEK), reappeared.visibility)
    }

    @Test
    fun `correction before interval elapses cancels waiting and returns to idle without reappearing`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow)
        engine.onMascotTapped(nowMillis = 0, reappearanceIntervalMillis = reappear)
        val corrected = engine.onCycle(80, ranges, nowMillis = 5_000, reappear, promptWindow)
        assertEquals(MascotVisibility.Hidden, corrected.visibility)

        val stillIdleLater = engine.onCycle(80, ranges, nowMillis = 90_000, reappear, promptWindow)
        assertEquals(MascotVisibility.Hidden, stillIdleLater.visibility)
    }

    @Test
    fun `prompt correction signal fires once when idle is reached within the window`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow) // enters Full Alert at t=0
        val corrected = engine.onCycle(80, ranges, nowMillis = 8_000, reappear, promptWindow) // corrected at t=8s, within 10s window
        assertTrue(corrected.promptCorrectionSignal)
    }

    @Test
    fun `prompt correction signal does not fire outside the window`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow)
        val corrected = engine.onCycle(80, ranges, nowMillis = 15_000, reappear, promptWindow) // 15s later, outside 10s window
        assertFalse(corrected.promptCorrectionSignal)
    }

    @Test
    fun `prompt correction signal only fires once per full-alert episode`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(10, ranges, nowMillis = 0, reappear, promptWindow)
        val first = engine.onCycle(80, ranges, nowMillis = 5_000, reappear, promptWindow)
        assertTrue(first.promptCorrectionSignal)

        // Dip back to full alert and correct again quickly — a fresh episode, should signal again.
        engine.onCycle(10, ranges, nowMillis = 10_000, reappear, promptWindow)
        val second = engine.onCycle(80, ranges, nowMillis = 12_000, reappear, promptWindow)
        assertTrue(second.promptCorrectionSignal)
    }

    @Test
    fun `alertJustAppeared fires once on transition to visible, not on every visible cycle`() {
        val engine = AdaptiveAlertEngine()
        val first = engine.onCycle(40, ranges, nowMillis = 0, reappear, promptWindow) // Peel, newly visible
        assertTrue(first.alertJustAppeared)
        val second = engine.onCycle(35, ranges, nowMillis = 100, reappear, promptWindow) // still Peel
        assertFalse(second.alertJustAppeared)
    }

    @Test
    fun `postureJustCorrected fires once on transition to idle`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(40, ranges, nowMillis = 0, reappear, promptWindow)
        val corrected = engine.onCycle(80, ranges, nowMillis = 100, reappear, promptWindow)
        assertTrue(corrected.postureJustCorrected)
        val stillIdle = engine.onCycle(90, ranges, nowMillis = 200, reappear, promptWindow)
        assertFalse(stillIdle.postureJustCorrected)
    }

    @Test
    fun `no signal when never having reached full alert`() {
        val engine = AdaptiveAlertEngine()
        engine.onCycle(60, ranges, nowMillis = 0, reappear, promptWindow) // Peek only
        val result = engine.onCycle(80, ranges, nowMillis = 1_000, reappear, promptWindow)
        assertFalse(result.promptCorrectionSignal)
    }
}
