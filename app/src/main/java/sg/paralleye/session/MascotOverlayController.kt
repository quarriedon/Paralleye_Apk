package sg.paralleye.session

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import sg.paralleye.config.MascotConfig
import sg.paralleye.domain.alert.AlertLevel
import sg.paralleye.domain.alert.MascotVisibility
import sg.paralleye.logging.ParallayeLogger
import sg.paralleye.ui.mascot.frameFor

/**
 * Ch.10 §12, §38: the mascot must be visible while PARALLEYE is backgrounded, not only while
 * [sg.paralleye.MainActivity] itself is on screen — that's what the app requests the
 * draw-over-other-apps permission for (`PermissionChecker.hasOverlayPermission`), which
 * previously gated monitoring startup without anything ever actually drawing an overlay
 * window with it.
 *
 * A plain [ImageView] via [WindowManager] rather than a `ComposeView` here — hosting Compose
 * outside an Activity needs its own manually-driven `LifecycleOwner`/`SavedStateRegistryOwner`
 * boilerplate, and this view's whole job is "show one of six static drawables, dismiss on
 * tap" — [sg.paralleye.ui.mascot.frameFor] already owns the level→drawable mapping, this class
 * only owns *how* to present it outside the app's own window.
 */
class MascotOverlayController(private val context: Context, private val config: MascotConfig) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ImageView? = null
    private var currentLevel: AlertLevel? = null

    var onTapped: (() -> Unit)? = null

    fun update(visibility: MascotVisibility) {
        when (visibility) {
            MascotVisibility.Hidden -> hide()
            is MascotVisibility.Visible -> show(visibility.level)
        }
    }

    private fun show(level: AlertLevel) {
        val view = overlayView ?: runCatching { createView() }
            .onFailure { ParallayeLogger.error("MascotOverlayController", "addView failed", it) }
            .getOrNull()?.also { overlayView = it }
            ?: return
        if (level != currentLevel) {
            view.setImageResource(frameFor(level))
            currentLevel = level
        }
        view.visibility = View.VISIBLE
    }

    fun hide() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
            .onFailure { ParallayeLogger.error("MascotOverlayController", "removeView failed", it) }
        overlayView = null
        currentLevel = null
    }

    private fun createView(): ImageView {
        val density = context.resources.displayMetrics.density
        val sizePx = (MASCOT_SIZE_DP * density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Ch.10 §13/§43: tap dismisses immediately, so touches on the mascot itself must
            // still be delivered — NOT_FOCUSABLE keeps it from stealing keyboard/input focus
            // from whatever app is in front; NOT_TOUCH_MODAL lets touches outside its bounds
            // pass straight through to that app.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (config.cornerOffsetXDp * density).toInt()
            y = (config.cornerOffsetYDp * density).toInt()
        }
        return ImageView(context).apply {
            setOnClickListener { onTapped?.invoke() }
            contentDescription = "Ms Angle Angel posture reminder"
            windowManager.addView(this, params)
        }
    }

    companion object {
        private const val MASCOT_SIZE_DP = 96
    }
}
