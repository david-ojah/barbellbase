package com.david.barbellbase.reminder

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import com.david.barbellbase.data.db.AlertSound
import com.david.barbellbase.data.db.ReminderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service that owns the looping alarm audio for water reminders.
 * It stops as soon as the ring cap is reached, so a missed alarm never rings forever.
 */
class ReminderAlertService : Service() {

    companion object {
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_LABEL = "label"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_RING_SECONDS = "ring_seconds"
        const val EXTRA_WATER_TOTAL = "water_total"
        const val EXTRA_WATER_TARGET = "water_target"
        const val EXTRA_WATER_AMOUNT = "water_amount"
        const val EXTRA_VIBRATE = "vibrate"
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private var stopJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var timedOut = false
    private var currentRuleId = -1
    private var currentKind = ReminderKind.WATER
    private var currentLabel = "Reminder"
    private var currentTotalMl = 0
    private var currentTargetMl = 2500
    private var currentAmountMl = 250

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ruleId = intent?.getIntExtra(EXTRA_RULE_ID, -1) ?: -1
        if (ruleId <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        val kind = runCatching {
            ReminderKind.valueOf(intent?.getStringExtra(EXTRA_KIND) ?: "WATER")
        }.getOrDefault(ReminderKind.WATER)
        val sound = runCatching {
            AlertSound.valueOf(intent?.getStringExtra(EXTRA_SOUND) ?: "ALARM")
        }.getOrDefault(AlertSound.ALARM)
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Reminder"
        val ringSeconds = (intent?.getIntExtra(EXTRA_RING_SECONDS, 60) ?: 60).coerceIn(5, 300)
        val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true

        timedOut = false
        currentRuleId = ruleId
        currentKind = kind
        currentLabel = label
        currentTotalMl = intent?.getIntExtra(EXTRA_WATER_TOTAL, 0) ?: 0
        currentTargetMl = intent?.getIntExtra(EXTRA_WATER_TARGET, 2500) ?: 2500
        currentAmountMl = intent?.getIntExtra(EXTRA_WATER_AMOUNT, 250) ?: 250

        val notification = if (kind == ReminderKind.WATER) {
            ReminderNotifications.buildWater(
                context = this,
                ruleId = ruleId,
                totalMl = intent?.getIntExtra(EXTRA_WATER_TOTAL, 0) ?: 0,
                targetMl = intent?.getIntExtra(EXTRA_WATER_TARGET, 2500) ?: 2500,
                amountMl = intent?.getIntExtra(EXTRA_WATER_AMOUNT, 250) ?: 250,
                sound = sound,
                ringing = true,
            )
        } else {
            ReminderNotifications.buildCustom(this, ruleId, label, sound, ringing = true)
        }

        startForeground(
            ReminderNotifications.notificationIdFor(kind, ruleId),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        acquireWakeLock(ringSeconds)
        if (sound == AlertSound.ALARM) {
            val ringer = (getSystemService(AUDIO_SERVICE) as AudioManager).ringerMode
            if (ringer == AudioManager.RINGER_MODE_NORMAL) {
                playAlarm()
            } else if (vibrate) {
                vibrateFor(ringSeconds)
            }
        }

        stopJob?.cancel()
        stopJob = scope.launch {
            delay(ringSeconds * 1000L)
            timedOut = true
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAlarm() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
            .also { audioManager.requestAudioFocus(it) }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(this@ReminderAlertService, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    private fun vibrateFor(seconds: Int) {
        val manager = getSystemService(VibratorManager::class.java) ?: return
        val vibrator = manager.defaultVibrator
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0),
        )
        scope.launch {
            delay(seconds * 1000L)
            vibrator.cancel()
        }
    }

    private fun acquireWakeLock(seconds: Int) {
        val power = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BarbellBase:alarm").apply {
            setReferenceCounted(false)
            acquire((seconds + 10) * 1000L)
        }
    }

    override fun onDestroy() {
        stopJob?.cancel()
        if (timedOut) repostQuietly()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
        focusRequest = null
        runCatching { (getSystemService(VibratorManager::class.java))?.defaultVibrator?.cancel() }
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    /**
     * When the ring cap is reached rather than a dismissal, leave a quiet notification behind so the
     * reminder is still visible in the shade instead of vanishing with the foreground service.
     */
    private fun repostQuietly() {
        if (currentRuleId <= 0) return
        if (!NotificationChannels.canPost(this)) return
        val notification = if (currentKind == ReminderKind.WATER) {
            ReminderNotifications.buildWater(
                context = this,
                ruleId = currentRuleId,
                totalMl = currentTotalMl,
                targetMl = currentTargetMl,
                amountMl = currentAmountMl,
                sound = AlertSound.SILENT,
                ringing = false,
            )
        } else {
            ReminderNotifications.buildCustom(
                this,
                currentRuleId,
                currentLabel,
                AlertSound.SILENT,
                ringing = false,
            )
        }
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(this)
                .notify(ReminderNotifications.notificationIdFor(currentKind, currentRuleId), notification)
        }
    }
}
