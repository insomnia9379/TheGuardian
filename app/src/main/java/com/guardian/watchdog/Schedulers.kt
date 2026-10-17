package com.guardian.watchdog

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Daily deep scan at 9am local.
 * Separate from the 5-min tripwire - this is the full audit.
 */
class DailyScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val findings = PrivilegeChecker.quickScan(context)
        val highs = findings.filter { it.severity == "HIGH" }

        if (highs.isNotEmpty()) {
            AlertHelper.sendHugeAlert(
                context,
                "🛡️ Guardian daily scan: ${highs.size} threat(s)",
                highs.joinToString("\n\n") { "[${it.area}] ${it.detail}" }
            )
        } else {
            AlertHelper.sendLowPriority(
                context,
                "Guardian daily scan: clean",
                "No monitoring apps found in any hiding spot. ${findings.size} info note(s)."
            )
        }
        AuditLog.append(context, "DAILY_SCAN: ${findings.size} finding(s), ${highs.size} HIGH")
        // Reschedule for tomorrow
        DailyScanScheduler.schedule(context)
    }
}

object DailyScanScheduler {
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, DailyScanReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next9am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // Inexact repeating is Doze-friendly; exact would need SCHEDULE_EXACT_ALARM
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next9am.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 2001, Intent(ctx, DailyScanReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}

/** Restarts the 5-min service + daily scan after reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, WatchdogForegroundService::class.java))
            DailyScanScheduler.schedule(context)
            AuditLog.append(context, "BOOT: watchdog restarted")
        }
    }
}
