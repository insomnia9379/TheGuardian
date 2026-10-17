package com.guardian.watchdog

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object AlertHelper {
    private const val CHANNEL_HIGH = "guardian_high"
    private const val CHANNEL_LOW = "guardian_low"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_HIGH, "Guardian Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Monitoring app installs and privilege escalations" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LOW, "Guardian Info", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun sendHighPriority(ctx: Context, title: String, body: String) {
        ensureChannels(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), n)
    }

    fun sendLowPriority(ctx: Context, title: String, body: String) {
        ensureChannels(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_LOW)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), n)
    }

    /**
     * HUGE notification: heads-up + vibration + full-screen intent.
     * This pops over whatever you're doing - you can't miss it.
     * Requires USE_FULL_SCREEN_INTENT permission on Android 14+.
     */
    fun sendHugeAlert(ctx: Context, title: String, body: String) {
        ensureChannels(ctx)

        // Full-screen intent: launches MainActivity over lock screen
        val fullScreenIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPi = PendingIntent.getActivity(
            ctx, 999,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(ctx, CHANNEL_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 1000))
            .setLights(0xFFFF0000.toInt(), 500, 500)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(
                android.R.drawable.ic_menu_view,
                "REVIEW NOW",
                fullScreenPi
            )
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Unique ID for huge alerts so they don't get bundled away
        nm.notify(9001, n)
    }
}
