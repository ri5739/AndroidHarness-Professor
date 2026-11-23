package com.androidharness.app.professor.persistence

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfessorRetryPolicyTest {
 @Test fun retryDelayDoublesAndCaps() {
  assertEquals(2_000L,ProfessorRetryPolicy.delay(0))
  assertEquals(4_000L,ProfessorRetryPolicy.delay(1))
  assertEquals(8_000L,ProfessorRetryPolicy.delay(2))
  assertEquals(900_000L,ProfessorRetryPolicy.delay(12))
 }
 @Test fun retryAtUsesCurrentTimestamp() {
  assertEquals(104_000L,ProfessorRetryPolicy.retryAt(100_000L,1))
 }
 @Test fun largeAttemptsCapAtMaximumDelay() {
  assertEquals(900_000L,ProfessorRetryPolicy.delay(20))
  assertEquals(900_000L,ProfessorRetryPolicy.delay(100))
 }
 @Test fun retryAtUsesCappedDelayForLargeAttempt() {
  assertEquals(1_900_000L,ProfessorRetryPolicy.retryAt(1_000_000L,100))
 }
 @Test(expected=IllegalArgumentException::class) fun negativeAttemptRejected() {
  ProfessorRetryPolicy.delay(-1)
 }
}
