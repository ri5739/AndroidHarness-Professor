package com.androidharness.app.professor

import android.content.Context

/**
 * Owner-controlled notification capture switches. Capture is opt-in per platform;
 * this preference never grants Android notification access or enables message sending.
 */
object ProfessorPlatformPreferences {
    private const val PREFS = "agent_professor_platforms"
    private fun key(platform: MessagingPlatform) = "capture_${platform.name.lowercase()}"

    fun isCaptureEnabled(context: Context, platform: MessagingPlatform): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(platform), false)

    fun setCaptureEnabled(context: Context, platform: MessagingPlatform, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(platform), enabled)
            .apply()
    }
}
