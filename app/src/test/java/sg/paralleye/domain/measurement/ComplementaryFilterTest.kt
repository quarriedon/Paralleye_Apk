package sg.paralleye.domain.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ComplementaryFilterTest {

    @Test
    fun `first update returns the accelerometer angle unchanged`() {
        val filter = ComplementaryFilter(alpha = 0.9)
        val result = filter.update(accelerometerAngleDegrees = 20.0, gyroChangeDegrees = 5.0)
        assertEquals(20.0, result, 0.0001)
    }

    @Test
    fun `subsequent update blends gyro-projected angle and accelerometer angle by alpha`() {
        val filter = ComplementaryFilter(alpha = 0.9)
        filter.update(accelerometerAngleDegrees = 20.0, gyroChangeDegrees = 0.0)
        val result = filter.update(accelerometerAngleDegrees = 20.0, gyroChangeDegrees = 2.0)
        // alpha * (previous + gyroChange) + (1-alpha) * accel = 0.9*(20+2) + 0.1*20 = 19.8 + 2.0 = 21.8
        assertEquals(21.8, result, 0.0001)
    }

    @Test
    fun `missing gyro data falls back to accelerometer angle only`() {
        val filter = ComplementaryFilter(alpha = 0.9)
        filter.update(accelerometerAngleDegrees = 20.0, gyroChangeDegrees = 0.0)
        val result = filter.update(accelerometerAngleDegrees = 30.0, gyroChangeDegrees = null)
        assertEquals(30.0, result, 0.0001)
    }

    @Test
    fun `reset clears fused state so next update is accelerometer-only again`() {
        val filter = ComplementaryFilter(alpha = 0.9)
        filter.update(accelerometerAngleDegrees = 20.0, gyroChangeDegrees = 5.0)
        filter.reset()
        val result = filter.update(accelerometerAngleDegrees = 40.0, gyroChangeDegrees = 10.0)
        assertEquals(40.0, result, 0.0001)
    }

    @Test
    fun `alpha outside zero-one range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ComplementaryFilter(alpha = 1.5) }
    }
}
