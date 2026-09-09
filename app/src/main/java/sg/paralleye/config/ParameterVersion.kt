package sg.paralleye.config

/**
 * Ch.2 §24 "Parameter-Version Principle": every build must record which parameter set it
 * shipped with, so pilot results from different builds are never silently compared as if
 * they used the same methodology.
 */
data class ParameterVersion(
    val configVersion: String,
    val buildVersionName: String,
) {
    companion object {
        val INITIAL = ParameterVersion(
            configVersion = "1.0.0-mvp",
            buildVersionName = "0.1.0-mvp",
        )
    }
}
