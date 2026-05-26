package com.GiaThinh.canlua.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

private const val TAG = "RefreshRateEffect"

/**
 * Unwrap ContextWrapper recursively to find the hosting Activity.
 * This is crucial in Jetpack Compose because the context returned by LocalView.current
 * or LocalContext.current is often wrapped in ContextThemeWrapper or Hilt wrappers.
 */
private tailrec fun Context.findActivity(): Activity? {
    if (this is Activity) return this
    if (this is ContextWrapper) return baseContext.findActivity()
    return null
}

/**
 * Request a lower display refresh rate (e.g. 60Hz) for battery saving on screens
 * that don't benefit from high refresh rates — like data entry or static content.
 *
 * On screens where the user primarily types numbers (WeightInputScreen) and
 * spends most of their session, dropping from 120Hz to 60Hz can save ~10-15%
 * battery without any perceptible quality loss.
 *
 * This sets [WindowManager.LayoutParams.preferredRefreshRate] as a **hint**
 * to the display subsystem. The OS may still override this if system animations
 * or other overlays require higher refresh rates.
 *
 * On dispose (leaving the screen), the preferred rate resets to 0 (system default),
 * restoring the device's native refresh rate for smooth scrolling on other screens.
 */
@Composable
fun RequestLowRefreshRate(targetHz: Float = 60f) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context.findActivity()
        if (activity == null) {
            Log.e(TAG, "RequestLowRefreshRate: Cannot resolve Activity from context: ${view.context}")
        }
        val window = activity?.window
        window?.let { w ->
            Log.d(TAG, "RequestLowRefreshRate: Setting preferredRefreshRate = $targetHz")
            w.attributes = w.attributes.apply {
                preferredRefreshRate = targetHz
            }
        }
        onDispose {
            window?.let { w ->
                Log.d(TAG, "RequestLowRefreshRate: Resetting preferredRefreshRate to 0 (default)")
                w.attributes = w.attributes.apply {
                    preferredRefreshRate = 0f
                }
            }
        }
    }
}

