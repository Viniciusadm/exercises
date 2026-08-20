package com.archieapps.exercises.core.rest

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mantém a contagem do descanso viva com o app em background: um serviço em primeiro plano evita
 * que o processo seja congelado (Android 12+) e a notificação mostra o cronômetro em contagem
 * regressiva, com ações de +15s e pular.
 */
class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notifications by lazy { RestNotifications(this) }
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifications.ensureChannels()

        // O sistema exige startForeground logo após startForegroundService, inclusive quando a
        // ação recebida vai encerrar o serviço em seguida (ex.: "pular" com o estado já vazio).
        promote(RestTimerBus.current ?: placeholder())

        when (intent?.action) {
            ACTION_START -> start(
                exerciseName = intent.getStringExtra(EXTRA_NAME).orEmpty(),
                seconds = intent.getIntExtra(EXTRA_SECONDS, DEFAULT_SECONDS),
            )

            ACTION_ADD -> {
                RestTimerBus.add(intent.getIntExtra(EXTRA_SECONDS, 15))
                val snapshot = RestTimerBus.current

                if (snapshot == null) finish(alert = false) else promote(snapshot)
            }

            ACTION_SKIP -> finish(alert = false)

            else -> finish(alert = false)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun start(exerciseName: String, seconds: Int) {
        notifications.cancelAlert()
        RestTimerBus.start(exerciseName, seconds)
        RestTimerBus.current?.let { promote(it) }

        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(500)
                val snapshot = RestTimerBus.current ?: break

                if (snapshot.isOver()) {
                    finish(alert = true, exerciseName = snapshot.exerciseName)
                    break
                }
            }
        }
    }

    private fun promote(snapshot: RestTimerBus.Snapshot) {
        runCatching { startForeground(RestNotifications.ONGOING_ID, notifications.ongoing(snapshot)) }
    }

    private fun placeholder() = RestTimerBus.Snapshot(
        exerciseName = "",
        totalSeconds = 1,
        endsAtMillis = System.currentTimeMillis(),
    )

    private fun finish(alert: Boolean, exerciseName: String? = null) {
        ticker?.cancel()
        val name = exerciseName ?: RestTimerBus.current?.exerciseName
        RestTimerBus.clear()

        if (alert) {
            vibrate()
            notifications.alert(name.orEmpty())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        runCatching {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 160, 120, 260), -1))
        }
    }

    companion object {
        const val ACTION_START = "com.archieapps.exercises.rest.START"
        const val ACTION_ADD = "com.archieapps.exercises.rest.ADD"
        const val ACTION_SKIP = "com.archieapps.exercises.rest.SKIP"
        const val EXTRA_NAME = "exercise_name"
        const val EXTRA_SECONDS = "seconds"
        const val DEFAULT_SECONDS = 90
    }
}

class RestTimerController(private val context: Context) {
    fun start(exerciseName: String, seconds: Int) = send(
        Intent(context, RestTimerService::class.java)
            .setAction(RestTimerService.ACTION_START)
            .putExtra(RestTimerService.EXTRA_NAME, exerciseName)
            .putExtra(RestTimerService.EXTRA_SECONDS, seconds)
    )

    fun add(seconds: Int) = send(
        Intent(context, RestTimerService::class.java)
            .setAction(RestTimerService.ACTION_ADD)
            .putExtra(RestTimerService.EXTRA_SECONDS, seconds)
    )

    fun skip() = send(
        Intent(context, RestTimerService::class.java).setAction(RestTimerService.ACTION_SKIP)
    )

    private fun send(intent: Intent) {
        runCatching { androidx.core.content.ContextCompat.startForegroundService(context, intent) }
    }
}
