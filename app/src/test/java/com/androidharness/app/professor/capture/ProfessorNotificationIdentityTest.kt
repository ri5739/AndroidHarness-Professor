package com.androidharness.app.professor.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProfessorNotificationIdentityTest {
    @Test fun accountScopeSeparatesAppPackages() {
        assertNotEquals(
            ProfessorNotificationIdentity.accountScope("com.whatsapp"),
            ProfessorNotificationIdentity.accountScope("com.whatsapp.w4b"),
        )
    }

    @Test fun conversationKeyPreferredOverDuplicateDisplayName() {
        val first = ProfessorNotificationIdentity.conversationId("notif-key-1", "Alex")
        val second = ProfessorNotificationIdentity.conversationId("notif-key-2", "Alex")
        assertNotEquals(first, second)
    }

    @Test fun sameNotificationIdentityProducesStableConversationId() {
        assertEquals(
            ProfessorNotificationIdentity.conversationId("notif-key-1", "Alex"),
            ProfessorNotificationIdentity.conversationId("notif-key-1", "Alex"),
        )
    }

    @Test fun messageIdChangesForNewNotificationPostTime() {
        val conversation = ProfessorNotificationIdentity.conversationId("notif-key-1", "Alex")
        assertNotEquals(
            ProfessorNotificationIdentity.messageId("com.whatsapp", conversation, "hello", 100L),
            ProfessorNotificationIdentity.messageId("com.whatsapp", conversation, "hello", 200L),
        )
    }
}
