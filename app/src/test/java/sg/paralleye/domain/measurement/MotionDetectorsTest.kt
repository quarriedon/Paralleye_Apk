package sg.paralleye.domain.measurement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientMotionDetectorTest {

    @Test
    fun `stable gravity and low angular velocity is not transient`() {
        val result = TransientMotionDetector.isTransient(
            angularVelocityMagnitude = 5.0,
            accelerationMagnitude = 9.8,
            maxAngularVelocityDegPerSec = 250.0,
            gravityToleranceMs2 = 2.5,
        )
        assertFalse(result)
    }

    @Test
    fun `excessive angular velocity is transient`() {
        val result = TransientMotionDetector.isTransient(
            angularVelocityMagnitude = 400.0,
            accelerationMagnitude = 9.8,
            maxAngularVelocityDegPerSec = 250.0,
            gravityToleranceMs2 = 2.5,
        )
        assertTrue(result)
    }

    @Test
    fun `acceleration far from gravity magnitude is transient`() {
        val result = TransientMotionDetector.isTransient(
            angularVelocityMagnitude = 5.0,
            accelerationMagnitude = 25.0,
            maxAngularVelocityDegPerSec = 250.0,
            gravityToleranceMs2 = 2.5,
        )
        assertTrue(result)
    }

    @Test
    fun `null angular velocity does not itself trigger transient state`() {
        val result = TransientMotionDetector.isTransient(
            angularVelocityMagnitude = null,
            accelerationMagnitude = 9.8,
            maxAngularVelocityDegPerSec = 250.0,
            gravityToleranceMs2 = 2.5,
        )
        assertFalse(result)
    }
}

class StablePostureQualifierTest {

    @Test
    fun `returns false until window is full`() {
        val qualifier = StablePostureQualifier(windowSize = 3, maxVariationDegrees = 2.0)
        assertFalse(qualifier.observe(10.0))
        assertFalse(qualifier.observe(10.0))
    }

    @Test
    fun `returns true once window is full and variation is within bound`() {
        val qualifier = StablePostureQualifier(windowSize = 3, maxVariationDegrees = 2.0)
        qualifier.observe(10.0)
        qualifier.observe(10.5)
        assertTrue(qualifier.observe(11.0))
    }

    @Test
    fun `returns false when variation exceeds bound`() {
        val qualifier = StablePostureQualifier(windowSize = 3, maxVariationDegrees = 2.0)
        qualifier.observe(10.0)
        qualifier.observe(20.0)
        assertFalse(qualifier.observe(11.0))
    }

    @Test
    fun `reset clears the window`() {
        val qualifier = StablePostureQualifier(windowSize = 2, maxVariationDegrees = 2.0)
        qualifier.observe(10.0)
        qualifier.observe(10.5)
        qualifier.reset()
        assertFalse(qualifier.observe(10.0))
    }
}
