package com.androidharness.app.professor.capture

import com.androidharness.app.professor.MessagingPlatform
import org.junit.Assert.*
import org.junit.Test

class ProfessorNotificationClassifierTest {
    @Test fun recognizesSupportedMessagingPackages() {
        assertEquals(MessagingPlatform.WHATSAPP_PERSONAL, ProfessorNotificationClassifier.platformForPackage("com.whatsapp"))
        assertEquals(MessagingPlatform.WHATSAPP_BUSINESS, ProfessorNotificationClassifier.platformForPackage("com.whatsapp.w4b"))
        assertEquals(MessagingPlatform.MESSENGER_PERSONAL, ProfessorNotificationClassifier.platformForPackage("com.facebook.orca"))
        assertEquals(MessagingPlatform.META_BUSINESS_MESSENGER, ProfessorNotificationClassifier.platformForPackage("com.facebook.pages.app"))
        assertEquals(MessagingPlatform.INSTAGRAM_DM, ProfessorNotificationClassifier.platformForPackage("com.instagram.android"))
    }

    @Test fun rejectsUnknownPackagesEmptyNotificationsAndNegativeTimestamp() {
        assertNull(ProfessorNotificationClassifier.normalize("com.example.app", "Title", "Body", 10L))
        assertNull(ProfessorNotificationClassifier.normalize("com.whatsapp", null, null, 10L))
        assertNull(ProfessorNotificationClassifier.normalize("com.whatsapp", "Title", "Body", -1L))
    }

    @Test fun trimsAndCombinesNotificationTextWithoutDuplicateTitle() {
        val notification = ProfessorNotificationClassifier.normalize("com.whatsapp", "  Alice  ", " Hello ", 123L)!!
        assertEquals("Alice", notification.title)
        assertEquals("Hello", notification.text)
        assertEquals("Alice\nHello", notification.body)
        assertEquals("Alice", notification.conversationHint)
        assertEquals(123L, notification.receivedAtEpochMillis)
    }

    @Test fun truncatesOversizedNotificationFields() {
        val notification = ProfessorNotificationClassifier.normalize("com.whatsapp", "A".repeat(5000), "B".repeat(5000), 1L)!!
        assertEquals(4000, notification.title?.length)
        assertEquals(4000, notification.text?.length)
        assertEquals(8001, notification.body.length)
    }

    @Test fun avoidsDuplicatingTitleWhenTextMatches() {
        val notification = ProfessorNotificationClassifier.normalize("com.whatsapp", "Alice", "Alice", 1L)!!
        assertEquals("Alice", notification.body)
    }
}
