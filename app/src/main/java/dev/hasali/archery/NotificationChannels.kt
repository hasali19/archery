package dev.hasali.archery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val SESSION = "session"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    SESSION,
                    "Active Session",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ),
        )
    }
}
