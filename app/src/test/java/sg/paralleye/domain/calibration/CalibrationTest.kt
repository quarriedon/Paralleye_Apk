package sg.paralleye.domain.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sg.paralleye.config.BaselineStatistic
import sg.paralleye.config.CalibrationConfig
import sg.paralleye.config.ParameterOrigin
import sg.paralleye.domain.measurement.MeasurementQuality
import sg.paralleye.domain.measurement.MeasurementSample
import sg.paralleye.domain.measurement.ScreenOrientation
import sg.paralleye.domain.measurement.SensorMode

class BaselineCalculatorTest {

    @Test
    fun `mean of valid samples`() {
        val result = BaselineCalculator.calculate(listOf(10.0, 20.0, 30.0), BaselineStatistic.MEAN)
        assertEquals(20.0, result!!, 0.0001)
    }

    @Test
    fun `median of odd-count samples`() {
        val result = BaselineCalculator.calculate(listOf(10.0, 50.0, 20.0), BaselineStatistic.MEDIAN)
        assertEquals(20.0, result!!, 0.0001)
    }

    @Test
    fun `median of even-count samples averages the middle two`() {
        val result = BaselineCalculator.calculate(listOf(10.0, 20.0, 30.0, 40.0), BaselineStatistic.MEDIAN)
        assertEquals(25.0, result!!, 0.0001)
    }

    @Test
    fun `trimmed mean drops outliers`() {
        val samples = listOf(1.0, 19.0, 20.0, 21.0, 100.0, 19.5, 20.5, 20.2, 19.8, 20.1)
        val mean = BaselineCalculator.calculate(samples, BaselineStatistic.MEAN)!!
        val trimmed = BaselineCalculator.calculate(samples, BaselineStatistic.TRIMMED_MEAN)!!
        assertTrue("trimmed mean should be closer to 20 than raw mean", kotlin.math.abs(trimmed - 20.0) < kotlin.math.abs(mean - 20.0))
    }

    @Test
    fun `empty sample list returns null`() {
        assertNull(BaselineCalculator.calculate(emptyList(), BaselineStatistic.MEAN))
    }
}

class BaselineCaptureSessionTest {

    private val config = CalibrationConfig(
        baselineStatistic = BaselineStatistic.MEAN,
        captureDurationMillis = 1000,
        minValidSamples = 3,
        origin = ParameterOrigin.ENGINEERING_DEFAULT_UNVALIDATED,
    )

    private fun sample(quality: MeasurementQuality, angle: Double? = 20.0) = MeasurementSample(
        timestampNanos = 0L,
        rawAccelerometer = null,
        rawGyroscope = null,
        screenOrientation = ScreenOrientation.PORTRAIT,
        deviceAngleDegrees = angle,
        estimatedNeckFlexionDegrees = null,
        angularVelocityMagnitude = null,
        quality = quality,
        sensorMode = SensorMode.FULL_FUSION,
    )

    @Test
    fun `succeeds with enough valid samples by capture duration end`() {
        val session = BaselineCaptureSession(config, methodologyVersion = "1.0", configurationVersion = "1.0")
        session.start(nowMillis = 0)
        session.observe(sample(MeasurementQuality.VALID, 18.0), nowMillis = 100)
        session.observe(sample(MeasurementQuality.VALID, 20.0), nowMillis = 200)
        session.observe(sample(MeasurementQuality.TRANSIENT_MOVEMENT, 45.0), nowMillis = 300)
        val outcome = session.observe(sample(MeasurementQuality.VALID, 22.0), nowMillis = 1000)

        assertTrue(outcome is BaselineCaptureOutcome.Success)
        val profile = (outcome as BaselineCaptureOutcome.Success).profile
        assertEquals(3, profile.validSampleCount)
        assertEquals(20.0, profile.guidedBaselineDeviceAngleDegrees, 0.0001)
        assertTrue(profile.isOriginalBaseline)
    }

    @Test
    fun `fails when insufficient valid samples at capture duration end`() {
        val session = BaselineCaptureSession(config, methodologyVersion = "1.0", configurationVersion = "1.0")
        session.start(nowMillis = 0)
        session.observe(sample(MeasurementQuality.VALID, 20.0), nowMillis = 100)
        val outcome = session.observe(sample(MeasurementQuality.TRANSIENT_MOVEMENT, 45.0), nowMillis = 1000)

        assertTrue(outcome is BaselineCaptureOutcome.Failure)
        assertEquals(
            BaselineCaptureFailureReason.INSUFFICIENT_VALID_SAMPLES,
            (outcome as BaselineCaptureOutcome.Failure).reason,
        )
    }

    @Test
    fun `invalid quality samples are not counted`() {
        val session = BaselineCaptureSession(config, methodologyVersion = "1.0", configurationVersion = "1.0")
        session.start(nowMillis = 0)
        session.observe(sample(MeasurementQuality.INVALID, null), nowMillis = 100)
        val outcome = session.observe(sample(MeasurementQuality.STABILISING, 20.0), nowMillis = 500)
        assertTrue(outcome is BaselineCaptureOutcome.InProgress)
        assertEquals(0, (outcome as BaselineCaptureOutcome.InProgress).validSampleCount)
    }
}

class UserProfileValidatorTest {

    @Test
    fun `valid profile passes`() {
        val result = UserProfileValidator.validate(UserProfile(ageYears = 14, gender = Gender.PREFER_NOT_TO_SAY, heightCm = 160))
        assertTrue(result is ProfileValidationResult.Valid)
    }

    @Test
    fun `age outside range is rejected`() {
        val result = UserProfileValidator.validate(UserProfile(ageYears = 200, gender = Gender.OTHER, heightCm = 160))
        assertTrue(result is ProfileValidationResult.Invalid)
    }

    @Test
    fun `height outside range is rejected`() {
        val result = UserProfileValidator.validate(UserProfile(ageYears = 14, gender = Gender.OTHER, heightCm = 5))
        assertTrue(result is ProfileValidationResult.Invalid)
    }
}
