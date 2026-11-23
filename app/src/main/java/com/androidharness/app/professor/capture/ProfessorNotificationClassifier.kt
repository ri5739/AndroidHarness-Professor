package com.androidharness.app.professor.capture

import com.androidharness.app.professor.MessagingPlatform

/**
 * Normalizes notification text only. The notification listener must still respect
 * user-granted access, privacy settings, and per-platform capture switches.
 */
data class CapturedNotification(
    val packageName: String,
    val title: String?,
    val text: String?,
    val receivedAtEpochMillis: Long,
    val conversationHint: String?,
) {
    val body: String
        get() = listOfNotNull(title?.trim()?.takeIf(String::isNotEmpty), text?.trim()?.takeIf(String::isNotEmpty))
            .distinct()
            .joinToString("\n")
}

object ProfessorNotificationClassifier {
    fun platformForPackage(packageName: String): MessagingPlatform? = when (packageName) {
        "com.whatsapp" -> MessagingPlatform.WHATSAPP_PERSONAL
        "com.whatsapp.w4b" -> MessagingPlatform.WHATSAPP_BUSINESS
        "com.facebook.orca" -> MessagingPlatform.MESSENGER_PERSONAL
        "com.facebook.pages.app" -> MessagingPlatform.META_BUSINESS_MESSENGER
        "com.instagram.android" -> MessagingPlatform.INSTAGRAM_DM
        else -> null
    }

    fun normalize(
        packageName: String,
        title: CharSequence?,
        text: CharSequence?,
        timestamp: Long,
    ): CapturedNotification? {
        if (platformForPackage(packageName) == null) return null
        val cleanTitle = title?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val cleanText = text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanTitle == null && cleanText == null) return null
        return CapturedNotification(
            packageName = packageName,
            title = cleanTitle,
            text = cleanText,
            receivedAtEpochMillis = timestamp,
            conversationHint = cleanTitle,
        )
    }
}
