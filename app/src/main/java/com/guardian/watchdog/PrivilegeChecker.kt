package com.guardian.watchdog

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Checks ALL the places stalkerware hides - not just the app list.
 * This is the "in all the areas" part you asked for.
 */
object PrivilegeChecker {

    data class Finding(val area: String, val detail: String, val severity: String)

    fun quickScan(ctx: Context): List<Finding> {
        val findings = mutableListOf<Finding>()

        // 1. Installed threat packages
        val pm = ctx.packageManager
        for (threat in ThreatDatabase.KNOWN_THREATS) {
            try {
                pm.getPackageInfo(threat.packageName, 0)
                findings.add(Finding(
                    "Installed app",
                    "${threat.label} [${threat.packageName}] is installed (${threat.kind})",
                    "HIGH"
                ))
            } catch (e: Exception) { /* not installed */ }
        }

        // 2. Device Admin apps - the "won't let me delete it" trick
        try {
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admins = dpm.activeAdmins ?: emptyList<ComponentName>()
            for (admin in admins) {
                val pkg = admin.packageName
                val threat = ThreatDatabase.lookup(pkg)
                val label = threat?.label ?: pkg
                findings.add(Finding(
                    "Device Admin",
                    "$label is a device admin - can block uninstall. Package: $pkg",
                    if (threat != null) "HIGH" else "MED"
                ))
            }
        } catch (e: Exception) {
            findings.add(Finding("Device Admin", "Could not query: ${e.message}", "LOW"))
        }

        // 3. Accessibility services - keylogging / screen reading
        try {
            val enabled = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            if (enabled.isNotEmpty()) {
                // Flag any accessibility service from a known threat
                for (threat in ThreatDatabase.KNOWN_THREATS) {
                    if (enabled.contains(threat.packageName, ignoreCase = true)) {
                        findings.add(Finding(
                            "Accessibility",
                            "${threat.label} has accessibility access - can read screen/input",
                            "HIGH"
                        ))
                    }
                }
                // Also note total count so user can review unknowns
                val count = enabled.split(":").size
                if (count > 0) {
                    findings.add(Finding(
                        "Accessibility",
                        "$count accessibility service(s) enabled. Review in Settings > Accessibility.",
                        "INFO"
                    ))
                }
            }
        } catch (e: Exception) {}

        // 4. Notification listeners - can read texts, WhatsApp, etc.
        try {
            val listeners = Settings.Secure.getString(
                ctx.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            for (threat in ThreatDatabase.KNOWN_THREATS) {
                if (listeners.contains(threat.packageName, ignoreCase = true)) {
                    findings.add(Finding(
                        "Notification access",
                        "${threat.label} can read notifications (texts, chats)",
                        "HIGH"
                    ))
                }
            }
        } catch (e: Exception) {}

        // 5. Unknown sources / adb - sideload vector
        try {
            val adb = Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0)
            if (adb == 1) {
                findings.add(Finding(
                    "Developer options",
                    "USB debugging (ADB) is ON - allows silent installs via PC",
                    "MED"
                ))
            }
        } catch (e: Exception) {}

        return findings
    }
}
