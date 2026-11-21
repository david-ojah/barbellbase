package com.david.barbellbase.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.david.barbellbase.MainActivity
import com.david.barbellbase.R
import com.david.barbellbase.reminder.NotificationChannels

object UpdateNotifications {
    private const val ID = 4101

    fun notify(context: Context, update: AvailableUpdate) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            context,
            ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.UPDATES)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle("BarbellBase ${update.version} is available")
            .setContentText("Tap to open Settings and install the update.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ID, notification)
        }
    }
}
