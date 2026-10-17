package com.guardian.watchdog

/**
 * Known monitoring / stalkerware package names.
 * Parent apps = watcher side, Kids/child apps = watched side.
 * If ANY of these appear without your consent, treat as compromise.
 */
object ThreatDatabase {
    data class Threat(
        val packageName: String,
        val label: String,
        val kind: String, // "parent" or "child" or "spyware"
        val notes: String
    )

    val KNOWN_THREATS = listOf(
        // AirDroid family - from your case
        Threat("com.sand.airdroidkidp", "AirDroid Parental Control", "parent",
            "Parent/wather app. If you didn't install it, someone set up watching from this phone."),
        Threat("com.sand.airdroidkids", "AirDroid Kids", "child",
            "Child/watched app. Uses device admin to block uninstall. If on your phone and you didn't consent, you are being watched."),
        Threat("com.sand.airdroid", "AirDroid: Remote Access", "spyware",
            "Remote control & file transfer - can be abused for surveillance."),
        Threat("com.sand.airmirror", "AirMirror", "spyware",
            "Remote control companion - high risk if not yours."),

        // Other common parental / monitoring apps abused as stalkerware
        Threat("com.familysafety", "Family Safety", "parent", "Parental control - verify consent."),
        Threat("com.kiddoware.kidsplace", "Kids Place", "child", "Kiosk/locker - can lock you out."),
        Threat("com.qustodio.qustodioapp", "Qustodio", "child", "Parental control child side."),
        Threat("com.netnanny", "Net Nanny", "child", "Content monitor child side."),
        Threat("com.mobicip", "Mobicip", "child", "Parental control."),
        Threat("com.bark.bark", "Bark", "parent", "Monitoring app."),
        Threat("com.mmguardian", "MMGuardian", "child", "Child monitor."),
        Threat("com.symantec.familysafety", "Norton Family", "child", "Child monitor."),

        // Classic stalkerware (non-consensual)
        Threat("com.mspy", "mSpy", "spyware", "Classic stalkerware - non-consensual tracking."),
        Threat("com.flexispy", "FlexiSPY", "spyware", "Classic stalkerware."),
        Threat("com.highster", "Highster Mobile", "spyware", "Stalkerware."),
        Threat("com.spyzie", "Spyzie", "spyware", "Stalkerware."),
        Threat("com.clevguard", "ClevGuard", "spyware", "Stalkerware family."),
        Threat("com.thecrawler", "TheTruthSpy", "spyware", "Stalkerware.")
    )

    fun lookup(pkg: String): Threat? = KNOWN_THREATS.find { it.packageName == pkg }

    // Heuristic keywords for unknown apps trying to hide
    val SUSPICIOUS_KEYWORDS = listOf(
        "parental", "kid", "child", "spy", "monitor", "track",
        "stealth", "hidden", "vault"
    )
}
