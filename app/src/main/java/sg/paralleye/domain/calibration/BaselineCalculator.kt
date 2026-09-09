package sg.paralleye.domain.calibration

import sg.paralleye.config.BaselineStatistic

/** Ch.4 §12: the exact baseline statistic is unresolved in the source documents — configurable, defaulted to mean. */
object BaselineCalculator {
    fun calculate(validAngleSamples: List<Double>, statistic: BaselineStatistic): Double? {
        if (validAngleSamples.isEmpty()) return null
        return when (statistic) {
            BaselineStatistic.MEAN -> validAngleSamples.average()
            BaselineStatistic.MEDIAN -> median(validAngleSamples)
            BaselineStatistic.TRIMMED_MEAN -> trimmedMean(validAngleSamples, trimFraction = 0.1)
        }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    private fun trimmedMean(values: List<Double>, trimFraction: Double): Double {
        val sorted = values.sorted()
        val trimCount = (sorted.size * trimFraction).toInt()
        val trimmed = sorted.subList(trimCount, sorted.size - trimCount)
        return if (trimmed.isEmpty()) sorted.average() else trimmed.average()
    }
}
