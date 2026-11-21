package com.david.barbellbase.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationManagerCompat
import com.david.barbellbase.data.db.AlertSound

/**
 * Channels are grouped by alert profile rather than by habit type, because Android 8+ does not let a
 * notification override its channel's sound. The alarm profile is silent at channel level: the looping
 * alarm is played by ReminderAlertService so it can stop on dismiss and never double up with a chime.
 */
object NotificationChannels {
    const val ALARM = "reminder_alarm"
    const val ALARM_FALLBACK = "reminder_alarm_fallback"
    const val CHIME = "reminder_chime"
    const val SILENT = "reminder_silent"
    const val REST = "rest_timer"
    const val WORKOUT = "workout_session"

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val alarm = NotificationChannel(
            ALARM,
            "Alarm reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Water reminders that ring like an alarm until dismissed"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            setShowBadge(true)
        }

        // Used only when the looping-alarm service cannot start: the channel itself provides one alarm sound.
        val fallback = NotificationChannel(
            ALARM_FALLBACK,
            "Alarm reminders (fallback)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarm sound used if the audio service is unavailable"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            setShowBadge(true)
        }

        val chime = NotificationChannel(
            CHIME,
            "Chime reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Walking and other gentle reminders"
            enableVibration(true)
            setShowBadge(true)
        }

        val silent = NotificationChannel(
            SILENT,
            "Silent reminders",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Reminders that appear without a sound"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val rest = NotificationChannel(
            REST,
            "Rest timer",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Countdown while you rest between sets"
            enableVibration(true)
            setShowBadge(false)
        }

        val workout = NotificationChannel(
            WORKOUT,
            "Active workout",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing workout status"
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(alarm, fallback, chime, silent, rest, workout))
    }

    fun channelFor(sound: AlertSound, fallbackAlarm: Boolean = false): String = when (sound) {
        AlertSound.ALARM -> if (fallbackAlarm) ALARM_FALLBACK else ALARM
        AlertSound.CHIME -> CHIME
        AlertSound.SILENT -> SILENT
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
