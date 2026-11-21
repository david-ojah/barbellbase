package com.david.barbellbase.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.david.barbellbase.MainActivity
import com.david.barbellbase.R
import com.david.barbellbase.data.db.AlertSound
import com.david.barbellbase.data.db.ReminderKind
import java.util.Locale

object ReminderNotifications {

    fun waterId(ruleId: Int) = 1000 + ruleId
    fun walkId(ruleId: Int) = 2000 + ruleId

    fun notificationIdFor(kind: ReminderKind, ruleId: Int): Int =
        if (kind == ReminderKind.WATER) waterId(ruleId) else walkId(ruleId)

    fun buildWater(
        context: Context,
        ruleId: Int,
        totalMl: Int,
        targetMl: Int,
        amountMl: Int,
        sound: AlertSound,
        ringing: Boolean,
        fallbackChannel: Boolean = false,
    ): android.app.Notification {
        val progress = if (targetMl > 0) (totalMl * 100 / targetMl).coerceIn(0, 100) else 0
        val text = String.format(
            Locale.US,
            "%.2f L of %.2f L today · %d%%",
            totalMl / 1000.0,
            targetMl / 1000.0,
            progress,
        )
        val builder = NotificationCompat.Builder(context, NotificationChannels.channelFor(sound, fallbackChannel))
            .setSmallIcon(R.drawable.ic_stat_water)
            .setContentTitle("Drink water")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setProgress(
                if (targetMl > 0) targetMl else 100,
                totalMl.coerceAtMost(if (targetMl > 0) targetMl else 100),
                false,
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(ringing)
            .setAutoCancel(!ringing)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context, ruleId))
            .addAction(
                0,
                "+$amountMl ml",
                ReminderActionReceiver.pending(
                    context,
                    ruleId,
                    ReminderActionReceiver.ACTION_LOG_WATER,
                    amountMl,
                ),
            )
            .addAction(
                0,
                "Stop",
                ReminderActionReceiver.pending(context, ruleId, ReminderActionReceiver.ACTION_STOP),
            )
        if (ringing) builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        return builder.build()
    }

    /** Walk chime notifications expire on their own instead of sitting in the shade until swiped. */
    private const val WALK_TIMEOUT_MS = 10 * 60 * 1000L

    fun buildWalk(
        context: Context,
        ruleId: Int,
        walkTodayMin: Int,
        targetMin: Int,
        suggestMin: Int,
        sound: AlertSound,
    ): android.app.Notification {
        val text = "$suggestMin min suggested · $walkTodayMin of $targetMin min today"
        val builder = NotificationCompat.Builder(context, NotificationChannels.channelFor(sound))
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle("Time for a walk")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setTimeoutAfter(WALK_TIMEOUT_MS)
            .setContentIntent(openApp(context, ruleId))
            .addAction(
                0,
                "Walked $suggestMin min",
                ReminderActionReceiver.pending(
                    context,
                    ruleId,
                    ReminderActionReceiver.ACTION_LOG_WALK,
                    suggestMin,
                ),
            )
            .addAction(
                0,
                "Snooze",
                ReminderActionReceiver.pending(context, ruleId, ReminderActionReceiver.ACTION_SNOOZE),
            )
        return builder.build()
    }

    /** Minimal notification for a custom reminder rule. */
    fun buildCustom(
        context: Context,
        ruleId: Int,
        label: String,
        sound: AlertSound,
        ringing: Boolean,
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(context, NotificationChannels.channelFor(sound))
            .setSmallIcon(R.drawable.ic_stat_water)
            .setContentTitle(label)
            .setContentText("Reminder")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(ringing)
            .setAutoCancel(!ringing)
            .setContentIntent(openApp(context, ruleId))
            .addAction(
                0,
                "Dismiss",
                ReminderActionReceiver.pending(context, ruleId, ReminderActionReceiver.ACTION_STOP),
            )
        return builder.build()
    }

    private fun openApp(context: Context, ruleId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ruleId", ruleId)
        }
        return PendingIntent.getActivity(
            context,
            ruleId * 10 + 5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun soundLabel(sound: AlertSound): String = when (sound) {
        AlertSound.ALARM -> "Alarm"
        AlertSound.CHIME -> "Chime"
        AlertSound.SILENT -> "Silent"
    }
}
