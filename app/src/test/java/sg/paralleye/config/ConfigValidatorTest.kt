package sg.paralleye.config

import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigValidatorTest {

    @Test
    fun `default provisional parameters are valid`() {
        val result = ConfigValidator.validate(ParallayeParameters.PROVISIONAL)
        assertTrue((result as? ValidationResult.Invalid)?.problems?.joinToString() ?: "", result is ValidationResult.Valid)
    }

    @Test
    fun `unordered zone thresholds are rejected`() {
        val bad = ParallayeParameters.PROVISIONAL.copy(
            zoneThresholds = ZoneThresholds(greenMaxDegrees = 30.0, yellowMaxDegrees = 25.0, hysteresisMarginDegrees = 0.5, origin = ParameterOrigin.ENGINEERING_DEFAULT_UNVALIDATED),
        )
        val result = ConfigValidator.validate(bad)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `overlapping angle-load bands are rejected`() {
        val bad = ParallayeParameters.PROVISIONAL.copy(
            angleLoadTable = AngleLoadTable(
                bands = listOf(
                    AngleLoadBand(0.0, 25.0, 0.0),
                    AngleLoadBand(20.0, null, 5.0),
                ),
                origin = ParameterOrigin.ENGINEERING_DEFAULT_UNVALIDATED,
            ),
        )
        val result = ConfigValidator.validate(bad)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `gap in angle-load bands is rejected`() {
        val bad = ParallayeParameters.PROVISIONAL.copy(
            angleLoadTable = AngleLoadTable(
                bands = listOf(
                    AngleLoadBand(0.0, 10.0, 0.0),
                    AngleLoadBand(20.0, null, 5.0),
                ),
                origin = ParameterOrigin.ENGINEERING_DEFAULT_UNVALIDATED,
            ),
        )
        val result = ConfigValidator.validate(bad)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `negative frame factor is rejected`() {
        val bad = ParallayeParameters.PROVISIONAL.copy(frameFactor = -0.01)
        val result = ConfigValidator.validate(bad)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `missing sensitivity preset is rejected`() {
        val bad = ParallayeParameters.PROVISIONAL.copy(
            sensitivityPresets = mapOf(SensitivityLevel.MEDIUM to SensitivityAdjustment.DEFAULTS.getValue(SensitivityLevel.MEDIUM)),
        )
        val result = ConfigValidator.validate(bad)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `angle-load lookup returns correct band`() {
        val table = AngleLoadTable.DEFAULT
        assertTrue(table.loadFor(0.0) == 0.0)
        assertTrue(table.loadFor(20.0) == 0.0)
        assertTrue(table.loadFor(21.0) == 2.0)
        assertTrue(table.loadFor(25.0) == 2.0)
        assertTrue(table.loadFor(26.0) == 4.0)
        assertTrue(table.loadFor(60.0) == 8.0)
        assertTrue(table.loadFor(90.0) == 10.0)
    }
}
