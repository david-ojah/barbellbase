package com.david.barbellbase.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.david.barbellbase.MainActivity
import com.david.barbellbase.R
import com.david.barbellbase.reminder.NotificationChannels

/**
 * Rest countdown is timestamp-based, so it does not need a foreground service. A notification with a
 * down-counting chronometer shows progress, and an alarm tells the user the moment the rest is over.
 */
object RestNotifications {
    const val ID = 42
    const val DONE_ID = 43

    fun show(context: Context, endAt: Long) {
        if (!NotificationChannels.canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val skip = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, RestActionReceiver::class.java).setAction(RestActionReceiver.ACTION_SKIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val plus = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, RestActionReceiver::class.java).setAction(RestActionReceiver.ACTION_PLUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val remainingMinutes = ((endAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, NotificationChannels.REST)
            .setSmallIcon(R.drawable.ic_stat_stopwatch)
            .setContentTitle("Resting")
            .setContentText("Next set ready in about $remainingMinutes min")
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(endAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "+15 s", plus)
            .addAction(0, "Skip", skip)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID, notification) }
    }

    fun done(context: Context) {
        cancel(context)
        if (!NotificationChannels.canPost(context)) return
        val notification: Notification = NotificationCompat.Builder(context, NotificationChannels.REST)
            .setSmallIcon(R.drawable.ic_stat_stopwatch)
            .setContentTitle("Rest over")
            .setContentText("Time for your next set.")
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 120, 200))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(DONE_ID, notification) }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(ID) }
    }
}
