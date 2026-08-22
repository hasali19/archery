package dev.hasali.archery.util

import android.content.Context
import android.content.Intent

/**
 * Restarts the app from scratch, relaunching its main activity in a fresh task and killing the
 * current process. Used after replacing the on-disk database, since the app's in-memory database
 * connections and any state read from them would otherwise be left referring to the old data.
 */
fun restartApp(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val component = launchIntent?.component ?: return

    context.startActivity(Intent.makeRestartActivityTask(component))
    Runtime.getRuntime().exit(0)
}
