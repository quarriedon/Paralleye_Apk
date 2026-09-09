package sg.paralleye.domain.measurement

import android.view.Surface

/** Ch.3 §3.4. Display orientation, independent of the physical sensor axes. */
enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE_LEFT,
    LANDSCAPE_RIGHT,
    REVERSE_PORTRAIT;

    companion object {
        /** Maps Android's [Surface] rotation constant (as reported by the Display) to our orientation enum. */
        fun fromSurfaceRotation(rotation: Int): ScreenOrientation = when (rotation) {
            Surface.ROTATION_0 -> PORTRAIT
            Surface.ROTATION_90 -> LANDSCAPE_LEFT
            Surface.ROTATION_180 -> REVERSE_PORTRAIT
            Surface.ROTATION_270 -> LANDSCAPE_RIGHT
            else -> PORTRAIT
        }
    }
}
