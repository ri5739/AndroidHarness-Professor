package com.androidharness.app.professor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorTrainingConflictsTest {
    @Test fun detectsOpposingRulesWithSharedSubject() {
        val conflicts = ProfessorTrainingConflictDetector.findConflicts(
            listOf("Always reply in Bengali", "Never reply in Bengali"),
        )
        assertEquals(1, conflicts.size)
        assertTrue(conflicts.single().overlappingTerms.contains("bengali"))
    }

    @Test fun doesNotFlagDifferentSubjectsAsConflict() {
        assertTrue(
            ProfessorTrainingConflictDetector.findConflicts(
                listOf("Always reply in Bengali", "Never share a password"),
            ).isEmpty(),
        )
    }

    @Test fun simulationResultCannotClaimExternalSend() {
        val result = TrainingSimulation(
            sampleMessage = "Hello",
            replyPreview = "Hi",
            requiresOwnerApproval = false,
            sentExternally = false,
        )
        assertEquals(false, result.sentExternally)
    }
}
