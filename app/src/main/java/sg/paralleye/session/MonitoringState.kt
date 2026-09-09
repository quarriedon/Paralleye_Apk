package sg.paralleye.session

/** Ch.11 §23 "Operational States". */
enum class MonitoringState {
    INITIALISATION,
    READY,
    MONITORING_ACTIVE,
    MONITORING_PAUSED,
    SESSION_COMPLETED,
}

/** Ch.11 §15, §20: what's blocking a transition out of Initialisation into Ready. */
sealed interface InitialisationOutcome {
    data object Ready : InitialisationOutcome
    data object OnboardingRequired : InitialisationOutcome
    data class PermissionsMissing(val missing: List<String>) : InitialisationOutcome
    data object ConfigurationInvalid : InitialisationOutcome
    data object SensorCapabilityUnavailable : InitialisationOutcome
}
