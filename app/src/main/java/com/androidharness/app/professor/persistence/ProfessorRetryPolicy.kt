package com.androidharness.app.professor.persistence
object ProfessorRetryPolicy {
 const val MAX_ATTEMPTS=6
 const val BASE_DELAY_MS=2000L
 const val MAX_DELAY_MS=900000L
 fun delay(attempt:Int):Long { require(attempt>=0); return (BASE_DELAY_MS*(1L shl attempt.coerceAtMost(20))).coerceAtMost(MAX_DELAY_MS) }
 fun retryAt(now:Long,attempt:Int)=now+delay(attempt)
}
