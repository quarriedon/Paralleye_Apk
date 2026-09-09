package sg.paralleye.domain.behaviour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sg.paralleye.config.ParameterOrigin
import sg.paralleye.config.RecoveryConfig
import sg.paralleye.config.ScoreConfig

class DynamicLoadEngineTest {

    @Test
    fun `at intended sampling rate reduces to the literal Ch1 formula`() {
        val increment = DynamicLoadEngine.calculateIncrement(
            angleLoad = 4.0,
            activityMultiplier = 1.3,
            frameFactor = 0.02,
            actualIntervalMillis = 20.0,
            intendedIntervalMillis = 20.0,
        )
        assertEquals(4.0 * 1.3 * 0.02, increment, 0.0001)
    }

    @Test
    fun `slower than intended sampling rate scales the increment up proportionally`() {
        val atIntendedRate = DynamicLoadEngine.calculateIncrement(4.0, 1.3, 0.02, 20.0, 20.0)
        val atHalfRate = DynamicLoadEngine.calculateIncrement(4.0, 1.3, 0.02, 40.0, 20.0)
        assertEquals(atIntendedRate * 2, atHalfRate, 0.0001)
    }

    @Test
    fun `zero or negative interval produces zero increment rather than a divide error`() {
        assertEquals(0.0, DynamicLoadEngine.calculateIncrement(4.0, 1.3, 0.02, 0.0, 20.0), 0.0001)
    }
}

class CumulativeLoadEngineTest {

    @Test
    fun `accumulates increments`() {
        val engine = CumulativeLoadEngine()
        engine.addIncrement(0.5)
        engine.addIncrement(0.3)
        assertEquals(0.8, engine.currentLoad, 0.0001)
    }

    @Test
    fun `subtraction never goes below zero`() {
        val engine = CumulativeLoadEngine()
        engine.addIncrement(1.0)
        engine.subtract(5.0)
        assertEquals(0.0, engine.currentLoad, 0.0001)
    }

    @Test
    fun `reset restores to zero by default`() {
        val engine = CumulativeLoadEngine()
        engine.addIncrement(10.0)
        engine.reset()
        assertEquals(0.0, engine.currentLoad, 0.0001)
    }
}

class RecoveryEngineTest {

    private val config = RecoveryConfig(
        activationDegrees = 20.0,
        rate = 0.3,
        promptCorrectionWindowSeconds = 10,
        promptCorrectionBonus = 5.0,
        origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION,
    )

    @Test
    fun `zero degrees produces maximum recovery of 0,3`() {
        assertEquals(0.3, RecoveryEngine.calculateRecovery(0.0, config), 0.0001)
    }

    @Test
    fun `five degrees produces approximately 0,225`() {
        assertEquals(0.225, RecoveryEngine.calculateRecovery(5.0, config), 0.001)
    }

    @Test
    fun `angle at or above threshold produces zero recovery`() {
        assertEquals(0.0, RecoveryEngine.calculateRecovery(20.0, config), 0.0001)
        assertEquals(0.0, RecoveryEngine.calculateRecovery(25.0, config), 0.0001)
    }

    @Test
    fun `qualification matches the activation threshold`() {
        assertTrue(RecoveryEngine.isRecoveryQualifying(19.9, config))
        assertFalse(RecoveryEngine.isRecoveryQualifying(20.0, config))
    }

    @Test
    fun `prompt correction bonus subtracts five with zero floor`() {
        val cumulative = CumulativeLoadEngine()
        cumulative.addIncrement(3.0)
        RecoveryEngine.applyPromptCorrectionBonus(cumulative, config)
        assertEquals(0.0, cumulative.currentLoad, 0.0001)
    }
}

class ScoringEngineTest {

    private val config = ScoreConfig(scalingFactor = 1.0, origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION)

    @Test
    fun `zero cumulative load produces score 100`() {
        assertEquals(100, ScoringEngine.calculateScore(0.0, config, monitoringActive = true))
    }

    @Test
    fun `cumulative load above 100 clamps score to zero, never negative`() {
        assertEquals(0, ScoringEngine.calculateScore(150.0, config, monitoringActive = true))
    }

    @Test
    fun `suspended monitoring produces no new score`() {
        assertNull(ScoringEngine.calculateScore(10.0, config, monitoringActive = false))
    }

    @Test
    fun `score reflects cumulative load directly at scaling factor 1`() {
        assertEquals(70, ScoringEngine.calculateScore(30.0, config, monitoringActive = true))
    }
}
