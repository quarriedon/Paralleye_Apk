package sg.paralleye.domain.behaviour

import org.junit.Assert.assertEquals
import org.junit.Test
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.config.ZoneThresholds
import sg.paralleye.domain.measurement.MeasurementQuality

class AngleInterpretationEngineTest {

    private val params = ParallayeParameters.PROVISIONAL

    @Test
    fun `23,4 degrees classifies Yellow with load 2, per Ch5 worked example`() {
        val result = AngleInterpretationEngine.interpret(23.4, MeasurementQuality.VALID, params)!!
        assertEquals(PostureZone.YELLOW, result.zone)
        assertEquals(2.0, result.angleLoad, 0.0001)
    }

    @Test
    fun `boundary 20,0 is Green, 20,1 is Yellow`() {
        val green = AngleInterpretationEngine.interpret(20.0, MeasurementQuality.VALID, params)!!
        val yellow = AngleInterpretationEngine.interpret(20.1, MeasurementQuality.VALID, params)!!
        assertEquals(PostureZone.GREEN, green.zone)
        assertEquals(PostureZone.YELLOW, yellow.zone)
    }

    @Test
    fun `boundary 25,0 is Yellow, 25,1 is Red`() {
        val yellow = AngleInterpretationEngine.interpret(25.0, MeasurementQuality.VALID, params)!!
        val red = AngleInterpretationEngine.interpret(25.1, MeasurementQuality.VALID, params)!!
        assertEquals(PostureZone.YELLOW, yellow.zone)
        assertEquals(PostureZone.RED, red.zone)
    }

    @Test
    fun `identical inputs always produce identical outputs regardless of call order`() {
        val a = AngleInterpretationEngine.interpret(30.0, MeasurementQuality.VALID, params)!!
        val b = AngleInterpretationEngine.interpret(30.0, MeasurementQuality.VALID, params)!!
        assertEquals(a, b)
    }
}

class ZoneTransitionGateTest {

    private val thresholds = ZoneThresholds.DEFAULT // green<=20, yellow<=25, margin=0.5

    @Test
    fun `small oscillation around boundary does not flip effective zone`() {
        val gate = ZoneTransitionGate(marginDegrees = thresholds.hysteresisMarginDegrees)
        assertEquals(PostureZone.GREEN, gate.observe(PostureZone.classify(20.0, thresholds), 20.0, thresholds))
        // 20.1 raw-classifies Yellow but hasn't cleared the 0.5 degree margin past 20.0
        assertEquals(PostureZone.GREEN, gate.observe(PostureZone.classify(20.1, thresholds), 20.1, thresholds))
        assertEquals(PostureZone.GREEN, gate.observe(PostureZone.classify(19.9, thresholds), 19.9, thresholds))
    }

    @Test
    fun `transition registers once margin is cleared`() {
        val gate = ZoneTransitionGate(marginDegrees = thresholds.hysteresisMarginDegrees)
        gate.observe(PostureZone.classify(20.0, thresholds), 20.0, thresholds)
        val result = gate.observe(PostureZone.classify(20.6, thresholds), 20.6, thresholds)
        assertEquals(PostureZone.YELLOW, result)
    }

    @Test
    fun `returning below the lower margin re-enters green`() {
        val gate = ZoneTransitionGate(marginDegrees = thresholds.hysteresisMarginDegrees)
        gate.observe(PostureZone.classify(20.6, thresholds), 20.6, thresholds) // enters yellow
        val stillYellow = gate.observe(PostureZone.classify(19.8, thresholds), 19.8, thresholds)
        assertEquals(PostureZone.YELLOW, stillYellow)
        val backToGreen = gate.observe(PostureZone.classify(19.4, thresholds), 19.4, thresholds)
        assertEquals(PostureZone.GREEN, backToGreen)
    }
}
