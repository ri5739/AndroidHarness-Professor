package com.androidharness.app.professor

import com.androidharness.app.professor.persistence.ProfessorTrainingTurnEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorTrainingCoachTest {
    @Test fun trainingContextPreservesConversationAndEndsWithCurrentOwnerTurn() {
        val history = listOf(
            ProfessorTrainingTurnEntity("1", "OWNER", "আমাকে বাংলায় উত্তর দেবে", 1L),
            ProfessorTrainingTurnEntity("2", "ASSISTANT", "ঠিক আছে, বাংলায় উত্তর দেব।", 2L),
            ProfessorTrainingTurnEntity("3", "OWNER", "উত্তর সংক্ষিপ্ত রাখবে", 3L),
        )

        val turns = buildTrainingConversationContext(history, "4", "এই নিয়মটিও মনে রাখো")

        assertEquals(4, turns.size)
        assertEquals(ProfessorTurnRole.USER, turns[0].role)
        assertEquals(ProfessorTurnRole.ASSISTANT, turns[1].role)
        assertEquals("এই নিয়মটিও মনে রাখো", turns.last().text)
        assertEquals(ProfessorTurnRole.USER, turns.last().role)
    }

    @Test fun trainingContextIsBoundedAndFiltersUnknownRolesAndSecrets() {
        val history = (1..40).map { index ->
            ProfessorTrainingTurnEntity("$index", if (index == 39) "SYSTEM" else "OWNER", "নিয়ম $index", index.toLong())
        }
        val turns = buildTrainingConversationContext(history, "41", "সর্বশেষ নির্দেশনা", maxTurns = 8)

        assertEquals(8, turns.size)
        assertEquals("সর্বশেষ নির্দেশনা", turns.last().text)
        assertTrue(turns.none { it.text == "নিয়ম 39" })
    }
}
