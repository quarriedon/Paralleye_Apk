package sg.paralleye.domain.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

private const val G = 9.80665f

class DeviceAngleCalculatorTest {

    @Test
    fun `vertical phone in portrait reads near zero degrees`() {
        // Held upright, screen facing user: gravity reaction almost entirely on device Y.
        val vertical = Vector3(0f, G, 0f)
        val angle = DeviceAngleCalculator.calculateAngleDegrees(vertical, ScreenOrientation.PORTRAIT)!!
        assertEquals(0.0, angle, 0.5)
    }

    @Test
    fun `flat face-up phone in portrait reads near ninety degrees`() {
        val flat = Vector3(0f, 0f, G)
        val angle = DeviceAngleCalculator.calculateAngleDegrees(flat, ScreenOrientation.PORTRAIT)!!
        assertEquals(90.0, angle, 0.5)
    }

    @Test
    fun `intermediate forty five degree tilt reads approximately forty five`() {
        val component = (G / sqrt(2.0)).toFloat()
        val midTilt = Vector3(0f, component, component)
        val angle = DeviceAngleCalculator.calculateAngleDegrees(midTilt, ScreenOrientation.PORTRAIT)!!
        assertEquals(45.0, angle, 1.0)
    }

    @Test
    fun `angle increases monotonically from vertical to horizontal`() {
        val angles = (0..90 step 10).map { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val y = (G * kotlin.math.cos(rad)).toFloat()
            val z = (G * kotlin.math.sin(rad)).toFloat()
            DeviceAngleCalculator.calculateAngleDegrees(Vector3(0f, y, z), ScreenOrientation.PORTRAIT)!!
        }
        for (i in 1 until angles.size) {
            assertTrue("angle should increase: ${angles[i - 1]} -> ${angles[i]}", angles[i] >= angles[i - 1] - 0.01)
        }
    }

    @Test
    fun `landscape left remap produces same angle for equivalent physical tilt`() {
        // LANDSCAPE_LEFT remap: x' = raw.y, y' = -raw.x. For remapped.y to read +G (the
        // "vertical" reading, same as portrait's Vector3(0,G,0)), raw.x must be -G.
        val verticalPortrait = Vector3(0f, G, 0f)
        val verticalLandscapeLeftRaw = Vector3(-G, 0f, 0f)
        val portraitAngle = DeviceAngleCalculator.calculateAngleDegrees(verticalPortrait, ScreenOrientation.PORTRAIT)!!
        val landscapeAngle = DeviceAngleCalculator.calculateAngleDegrees(verticalLandscapeLeftRaw, ScreenOrientation.LANDSCAPE_LEFT)!!
        assertEquals(portraitAngle, landscapeAngle, 0.5)
    }

    @Test
    fun `zero vector is rejected as invalid`() {
        val angle = DeviceAngleCalculator.calculateAngleDegrees(Vector3(0f, 0f, 0f), ScreenOrientation.PORTRAIT)
        assertNull(angle)
    }

    @Test
    fun `NaN input is rejected as invalid`() {
        val angle = DeviceAngleCalculator.calculateAngleDegrees(Vector3(Float.NaN, G, 0f), ScreenOrientation.PORTRAIT)
        assertNull(angle)
    }

    @Test
    fun `ratio slightly outside unit range is clamped rather than producing NaN`() {
        // Magnitude computed from x,y,z but y alone slightly exceeds magnitude due to float error.
        val nearUnity = Vector3(0.001f, G, 0.0001f)
        val angle = DeviceAngleCalculator.calculateAngleDegrees(nearUnity, ScreenOrientation.PORTRAIT)
        assertTrue(angle != null && angle.isFinite())
    }

    @Test
    fun `gravity magnitude validation accepts resting device`() {
        assertTrue(DeviceAngleCalculator.isGravityMagnitudeValid(Vector3(0f, G, 0f)))
    }

    @Test
    fun `gravity magnitude validation rejects strong linear movement`() {
        assertTrue(!DeviceAngleCalculator.isGravityMagnitudeValid(Vector3(0f, G * 3, 0f)))
    }

    @Test
    fun `remap identity for portrait`() {
        val v = Vector3(1f, 2f, 3f)
        assertEquals(v, DeviceAngleCalculator.remapForOrientation(v, ScreenOrientation.PORTRAIT))
    }

    @Test
    fun `remap reverse portrait negates x and y`() {
        val v = Vector3(1f, 2f, 3f)
        val remapped = DeviceAngleCalculator.remapForOrientation(v, ScreenOrientation.REVERSE_PORTRAIT)
        assertEquals(-1f, remapped.x, 0.001f)
        assertEquals(-2f, remapped.y, 0.001f)
        assertEquals(3f, remapped.z, 0.001f)
    }

    @Test
    fun `landscape left and landscape right remaps are inverses`() {
        val v = Vector3(1f, 2f, 3f)
        val left = DeviceAngleCalculator.remapForOrientation(v, ScreenOrientation.LANDSCAPE_LEFT)
        val roundTrip = DeviceAngleCalculator.remapForOrientation(left, ScreenOrientation.LANDSCAPE_RIGHT)
        assertEquals(v.x, roundTrip.x, 0.001f)
        assertEquals(v.y, roundTrip.y, 0.001f)
        assertEquals(v.z, roundTrip.z, 0.001f)
    }
}
