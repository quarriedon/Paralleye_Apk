package sg.paralleye.domain.reporting

import org.junit.Assert.assertEquals
import org.junit.Test
import sg.paralleye.config.ActivityCategory
import sg.paralleye.domain.behaviour.PostureZone

class SessionSummaryAccumulatorTest {

    @Test
    fun `accumulates zone and activity durations`() {
        val acc = SessionSummaryAccumulator()
        acc.start(0)
        acc.observeCycle(PostureZone.GREEN, ActivityCategory.SCROLLING, 1000, 90, 5.0, 0.0, false, false)
        acc.observeCycle(PostureZone.GREEN, ActivityCategory.SCROLLING, 1000, 90, 5.0, 0.0, false, false)
        acc.observeCycle(PostureZone.YELLOW, ActivityCategory.TYPING, 2000, 80, 8.0, 0.0, false, false)
        val summary = acc.finish(5000)

        assertEquals(2000L, summary.zoneDurationsMillis[PostureZone.GREEN])
        assertEquals(2000L, summary.zoneDurationsMillis[PostureZone.YELLOW])
        assertEquals(2000L, summary.activityDurationsMillis[ActivityCategory.SCROLLING])
        assertEquals(2000L, summary.activityDurationsMillis[ActivityCategory.TYPING])
    }

    @Test
    fun `tracks score statistics and final score`() {
        val acc = SessionSummaryAccumulator()
        acc.start(0)
        acc.observeCycle(PostureZone.GREEN, ActivityCategory.UNKNOWN, 1000, 100, 0.0, 0.0, false, false)
        acc.observeCycle(PostureZone.RED, ActivityCategory.UNKNOWN, 1000, 40, 60.0, 0.0, false, false)
        acc.observeCycle(PostureZone.YELLOW, ActivityCategory.UNKNOWN, 1000, 70, 30.0, 0.0, false, false)
        val summary = acc.finish(3000)

        assertEquals(70, summary.finalScore)
        assertEquals(100, summary.maxScore)
        assertEquals(40, summary.minScore)
        assertEquals(70.0, summary.averageScore, 0.001)
        assertEquals(60.0, summary.peakCumulativeLoad, 0.001)
    }

    @Test
    fun `counts alert and correction events only on their trigger cycle`() {
        val acc = SessionSummaryAccumulator()
        acc.start(0)
        acc.observeCycle(PostureZone.RED, ActivityCategory.UNKNOWN, 1000, 20, 80.0, 0.0, alertJustAppeared = true, postureJustCorrected = false)
        acc.observeCycle(PostureZone.RED, ActivityCategory.UNKNOWN, 1000, 20, 80.0, 0.0, alertJustAppeared = false, postureJustCorrected = false)
        acc.observeCycle(PostureZone.GREEN, ActivityCategory.UNKNOWN, 1000, 90, 0.0, 5.0, alertJustAppeared = false, postureJustCorrected = true)
        val summary = acc.finish(3000)

        assertEquals(1, summary.alertCount)
        assertEquals(1, summary.correctionEventCount)
        assertEquals(5.0, summary.totalRecoveryAchieved, 0.001)
    }

    @Test
    fun `empty session defaults to neutral values rather than crashing`() {
        val acc = SessionSummaryAccumulator()
        acc.start(0)
        val summary = acc.finish(0)
        assertEquals(100, summary.finalScore)
        assertEquals(0, summary.alertCount)
    }

    @Test
    fun `start resets state from a previous session`() {
        val acc = SessionSummaryAccumulator()
        acc.start(0)
        acc.observeCycle(PostureZone.RED, ActivityCategory.GAMING, 5000, 10, 90.0, 0.0, true, false)
        acc.start(10_000)
        val summary = acc.finish(11_000)
        assertEquals(100, summary.finalScore)
        assertEquals(0, summary.alertCount)
        assertEquals(emptyMap<PostureZone, Long>(), summary.zoneDurationsMillis)
    }
}
