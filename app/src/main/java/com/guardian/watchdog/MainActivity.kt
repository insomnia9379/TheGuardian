package com.guardian.watchdog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple UI: big SCAN button, results list, audit log.
 * Built to be usable at 3am when you're stressed - no clutter.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var resultsView: TextView
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertHelper.ensureChannels(this)

        // Android 13+ hides every alert until the user grants notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Start 5-min watchdog + daily 9am scan
        startForegroundService(Intent(this, WatchdogForegroundService::class.java))
        DailyScanScheduler.schedule(this)
        AuditLog.append(this, "APP_START: 5-min service + daily scan scheduled")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "🛡️ Guardian Watchdog"
            textSize = 24f
        }
        layout.addView(title)

        val sub = TextView(this).apply {
            text = "Watches for monitoring apps in all the hiding spots."
            textSize = 14f
        }
        layout.addView(sub)

        val scanBtn = Button(this).apply {
            text = "SCAN NOW"
            setOnClickListener { runScan() }
        }
        layout.addView(scanBtn)

        resultsView = TextView(this).apply {
            text = "Tap SCAN NOW"
            textSize = 14f
        }
        layout.addView(resultsView)

        val logTitle = TextView(this).apply {
            text = "\nAudit log:"
            textSize = 16f
        }
        layout.addView(logTitle)

        logView = TextView(this).apply {
            textSize = 12f
        }
        val scroll = ScrollView(this).apply { addView(logView) }
        layout.addView(scroll)

        val clearBtn = Button(this).apply {
            text = "Clear log"
            setOnClickListener {
                AuditLog.clear(this@MainActivity)
                refreshLog()
            }
        }
        layout.addView(clearBtn)

        setContentView(layout)
        runScan()
    }

    private fun runScan() {
        val findings = PrivilegeChecker.quickScan(this)
        if (findings.isEmpty()) {
            resultsView.text = "✅ Clean - no known monitoring apps, no suspicious admins."
        } else {
            val sb = StringBuilder()
            for (f in findings) {
                val icon = when (f.severity) {
                    "HIGH" -> "🔴"
                    "MED" -> "🟡"
                    else -> "ℹ️"
                }
                sb.append("$icon [${f.area}] ${f.detail}\n\n")
            }
            // Alert on highs
            val highs = findings.filter { it.severity == "HIGH" }
            if (highs.isNotEmpty()) {
                AlertHelper.sendHighPriority(
                    this,
                    "Guardian found ${highs.size} threat(s)",
                    highs.joinToString("\n") { it.detail }
                )
            }
            resultsView.text = sb.toString()
        }
        AuditLog.append(this, "MANUAL_SCAN: ${findings.size} finding(s)")
        refreshLog()
    }

    private fun refreshLog() {
        logView.text = AuditLog.read(this)
    }
}
