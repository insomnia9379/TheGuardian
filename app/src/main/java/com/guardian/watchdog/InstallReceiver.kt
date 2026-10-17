package com.guardian.watchdog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Fires on every app install / update.
 * This is the core tripwire - catches AirDroid-style installs
 * the second they happen, including auto-restore after factory reset.
 */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) return

        val pkg = intent.data?.schemeSpecificPart ?: return
        val isUpdate = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (isUpdate) return // only care about fresh installs

        Log.i("Guardian", "Package installed: $pkg")

        val threat = ThreatDatabase.lookup(pkg)
        if (threat != null) {
            AlertHelper.sendHighPriority(
                context,
                "⚠️ Monitoring app installed",
                "${threat.label} (${threat.kind}) was just installed. " +
                "If you didn't do this, your phone may be compromised. Tap to review."
            )
            // Log for audit trail
            AuditLog.append(context, "INSTALL_DETECTED: ${threat.label} [$pkg] kind=${threat.kind}")
        } else {
            // Heuristic check for sneaky names
            val lower = pkg.lowercase()
            if (ThreatDatabase.SUSPICIOUS_KEYWORDS.any { lower.contains(it) }) {
                AlertHelper.sendLowPriority(
                    context,
                    "New app with suspicious name",
                    "$pkg was installed and looks like monitoring software. Tap to review."
                )
                AuditLog.append(context, "SUSPICIOUS_INSTALL: $pkg")
            }
        }

        // Always re-check privileged vectors after any install
        PrivilegeChecker.quickScan(context)
    }
}
