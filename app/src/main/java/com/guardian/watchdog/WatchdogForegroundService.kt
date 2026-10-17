package com.guardian.watchdog

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * Runs every 5 minutes in the foreground.
 * Android kills background work aggressively (Doze), so a foreground
 * service with a persistent notification is the only reliable way
 * to get a true 5-minute cadence without system privileges.
 */
class WatchdogForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val intervalMs = 5 * 60 * 1000L // 5 minutes

    private val checker = object : Runnable {
        override fun run() {
            val findings = PrivilegeChecker.quickScan(this@WatchdogForegroundService)
            val highs = findings.filter { it.severity == "HIGH" }
            if (highs.isNotEmpty()) {
                // Huge alert, not a quiet ping
                AlertHelper.sendHugeAlert(
                    this@WatchdogForegroundService,
                    "🛡️ GUARDIAN: Threat found",
                    highs.joinToString("\n\n") { "[${it.area}] ${it.detail}" }
                )
                AuditLog.append(
                    this@WatchdogForegroundService,
                    "5MIN_SCAN: ${highs.size} HIGH finding(s): " +
                    highs.joinToString("; ") { it.detail }
                )
            } else {
                AuditLog.append(this@WatchdogForegroundService, "5MIN_SCAN: clean")
            }
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AlertHelper.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val persistent = NotificationCompat.Builder(this, "guardian_low")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Guardian Watchdog running")
            .setContentText("Checking every 5 min for monitoring apps")
            .setOngoing(true)
            .build()
        // Use a fixed ID scheme: 1001 = persistent
        startForeground(1001, persistent)

        handler.removeCallbacks(checker)
        handler.post(checker) // run now, then every 5 min

        return START_STICKY // restart if killed
    }

    override fun onDestroy() {
        handler.removeCallbacks(checker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
