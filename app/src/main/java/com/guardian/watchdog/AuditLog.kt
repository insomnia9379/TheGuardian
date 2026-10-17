package com.guardian.watchdog

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple append-only audit trail so you have proof of when things were installed. */
object AuditLog {
    private const val PREFS = "guardian_audit"
    private const val KEY = "log"

    fun append(ctx: Context, entry: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val prev = prefs.getString(KEY, "") ?: ""
        prefs.edit().putString(KEY, "$prev\n[$ts] $entry").apply()
    }

    fun read(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, "(no events yet)") ?: "(no events yet)"
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
