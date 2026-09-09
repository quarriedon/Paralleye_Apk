package sg.paralleye.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Ch.10 mascot must appear as a system-level overlay while the app is backgrounded, but
 * must not duplicate the in-app [sg.paralleye.ui.mascot.MsAngleAngelOverlay] while
 * [sg.paralleye.MainActivity] is actually visible. Whether to show the overlay is
 * [sg.paralleye.session.MonitoringForegroundService]'s decision, not any one Activity's, so
 * visibility is tracked centrally here (via [sg.paralleye.ParallayeApplication]'s
 * `ActivityLifecycleCallbacks`) rather than each screen managing its own state.
 */
object AppVisibilityTracker {
    private val _isAppVisible = MutableStateFlow(false)
    val isAppVisible: StateFlow<Boolean> = _isAppVisible.asStateFlow()

    private var startedActivityCount = 0

    fun onActivityStarted() {
        startedActivityCount++
        _isAppVisible.value = startedActivityCount > 0
    }

    fun onActivityStopped() {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        _isAppVisible.value = startedActivityCount > 0
    }
}
