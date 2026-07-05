package dev.hasali.archery

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import dev.hasali.archery.data.Session
import dev.hasali.archery.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ActiveSessionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var sessionRepository: SessionRepository
    private lateinit var wearPublisher: WearSessionPublisher

    override fun onCreate() {
        super.onCreate()
        val application = this.application as ArcheryApplication
        sessionRepository = application.sessionRepository
        wearPublisher = WearSessionPublisher(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val sessionId = intent!!.getIntExtra("sessionId", 0)

        val foregroundServiceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(sessionId),
            foregroundServiceType,
        )

        val notificationManager = NotificationManagerCompat.from(this)

        serviceScope.launch {
            sessionRepository
                .watchSession(sessionId)
                .collect { session ->
                    if (session == null) {
                        stopSelf()
                        return@collect
                    }
                    if (ActivityCompat.checkSelfPermission(
                            this@ActiveSessionService,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(sessionId, session))
                    }
                    wearPublisher.publish(sessionId, session)
                }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wearPublisher.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun sessionPendingIntent(sessionId: Int): PendingIntent {
        val deepLinkUri = "archery://session/$sessionId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri, this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            sessionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildNotification(sessionId: Int): Notification =
        NotificationCompat
            .Builder(this, NotificationChannels.SESSION)
            .setContentTitle("Active Session")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .setContentIntent(sessionPendingIntent(sessionId))
            .build()

    private fun buildNotification(
        sessionId: Int,
        session: Session,
    ): Notification {
        val total = session.scores.sumOf { it.value }
        val average = if (session.scores.isEmpty()) 0.0 else total.toDouble() / session.scores.size

        return NotificationCompat
            .Builder(this, NotificationChannels.SESSION)
            .setContentTitle(session.roundDetails.displayName)
            .setContentText("Total: $total\tAverage: ${"%.2f".format(average)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .setContentIntent(sessionPendingIntent(sessionId))
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}
