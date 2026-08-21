package com.archieapps.exercises.core.rest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.archieapps.exercises.MainActivity
import com.archieapps.exercises.R

class RestNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            "Descanso em andamento",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
        }

        val alert = NotificationChannel(
            CHANNEL_ALERT,
            "Fim do descanso",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 160, 120, 260)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        val service = context.getSystemService(NotificationManager::class.java)
        service?.createNotificationChannel(ongoing)
        service?.createNotificationChannel(alert)
    }

    fun ongoing(snapshot: RestTimerBus.Snapshot): Notification =
        NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setContentTitle("Descanso")
            .setContentText(snapshot.exerciseName)
            .setWhen(snapshot.endsAtMillis)
            .setUsesChronometer(true)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) setChronometerCountDown(true) }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setSilent(true)
            .setContentIntent(openSession(REQUEST_ONGOING))
            .addAction(0, "+15s", serviceAction(RestTimerService.ACTION_ADD))
            .addAction(0, "Pular", serviceAction(RestTimerService.ACTION_SKIP))
            .build()

    fun alert(exerciseName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setContentTitle("Descanso terminou")
            .setContentText("Próxima série: $exerciseName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openSession(REQUEST_ALERT))
            .build()

        runCatching { manager.notify(ALERT_ID, notification) }
    }

    fun cancelAlert() = runCatching { manager.cancel(ALERT_ID) }

    private fun openSession(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_SESSION)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, RestTimerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ONGOING = "rest_ongoing"
        const val CHANNEL_ALERT = "rest_alert"
        const val ONGOING_ID = 4201
        const val ALERT_ID = 4202
        const val ACTION_OPEN_SESSION = "com.archieapps.exercises.OPEN_SESSION"

        private const val REQUEST_ONGOING = 1
        private const val REQUEST_ALERT = 2
    }
}
