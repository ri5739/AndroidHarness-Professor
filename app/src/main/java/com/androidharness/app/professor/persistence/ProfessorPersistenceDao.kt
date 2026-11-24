package com.androidharness.app.professor.persistence
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class ProfessorScopeRow(val platform: String, val accountId: String, val conversationId: String)

@Entity(tableName="professor_training_lessons", indices=[Index(value=["state","updatedAt"]), Index(value=["scopePlatform","accountId","conversationId"])])
data class ProfessorTrainingLessonEntity(
    @PrimaryKey val lessonId: String,
    val rule: String,
    val scopePlatform: String?,
    val accountId: String?,
    val conversationId: String?,
    val state: String = "PROPOSED",
    val createdAt: Long,
    val updatedAt: Long,
    val approvedAt: Long? = null,
    @ColumnInfo(defaultValue = "1") val revision: Int = 1,
)

@Entity(tableName = "professor_training_turns", indices = [Index("createdAt")])
data class ProfessorTrainingTurnEntity(
    @PrimaryKey val turnId: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val scopePlatform: String? = null,
    val accountId: String? = null,
    val conversationId: String? = null,
)

@Entity(tableName = "professor_training_audit", indices = [Index("lessonId"), Index("occurredAt")])
data class ProfessorTrainingAuditEntity(
 @PrimaryKey val auditId: String,
 val lessonId: String,
 val action: String,
 val revision: Int,
 val ruleBefore: String,
 val ruleAfter: String,
 val occurredAt: Long,
)

@Entity(tableName="professor_incoming", primaryKeys=["platform","accountId","conversationId","messageId"], indices=[Index(value=["status","nextRetryAt","receivedAt"])])
data class ProfessorIncomingEntity(val platform:String,val accountId:String,val conversationId:String,val messageId:String,val senderId:String,val text:String,val receivedAt:Long,val status:String="QUEUED",val attempts:Int=0,val nextRetryAt:Long=0,val lastError:String?=null,val insertedAt:Long=System.currentTimeMillis(),val updatedAt:Long=System.currentTimeMillis())

@Entity(tableName="professor_drafts",indices=[Index(value=["platform","accountId","conversationId","updatedAt"])])
data class ProfessorDraftEntity(@PrimaryKey val draftId:String,val platform:String,val accountId:String,val conversationId:String,val inReplyToMessageId:String,val text:String,val status:String="PENDING",val riskFlagsJson:String="[]",val confidence:Float=0f,val createdAt:Long=System.currentTimeMillis(),val updatedAt:Long=System.currentTimeMillis())

@Dao interface ProfessorPersistenceDao {
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun enqueue(message:ProfessorIncomingEntity):Long
 @Query("SELECT * FROM professor_incoming WHERE platform=:p AND accountId=:a AND conversationId=:c AND status IN ('QUEUED','RETRY') AND nextRetryAt<=:now ORDER BY receivedAt,insertedAt LIMIT 1") suspend fun nextDue(p:String,a:String,c:String,now:Long):ProfessorIncomingEntity?
 @Query("UPDATE professor_incoming SET status='PROCESSING',updatedAt=:now WHERE platform=:p AND accountId=:a AND conversationId=:c AND messageId=:id AND status IN ('QUEUED','RETRY') AND nextRetryAt<=:now") suspend fun claim(p:String,a:String,c:String,id:String,now:Long):Int
 @Transaction suspend fun claimNext(p:String,a:String,c:String,now:Long):ProfessorIncomingEntity? { val row=nextDue(p,a,c,now)?:return null; if(claim(p,a,c,row.messageId,now)!=1)return null; return row.copy(status="PROCESSING",updatedAt=now) }
 @Query("SELECT attempts FROM professor_incoming WHERE platform=:p AND accountId=:a AND conversationId=:c AND messageId=:id AND status='PROCESSING'") suspend fun processingAttempts(p:String,a:String,c:String,id:String):Int?
 @Query("SELECT * FROM professor_incoming WHERE platform=:p AND accountId=:a AND conversationId=:c ORDER BY receivedAt,insertedAt") fun messagesFlow(p:String,a:String,c:String):Flow<List<ProfessorIncomingEntity>>
 @Query("SELECT * FROM professor_incoming WHERE platform=:p AND accountId=:a AND conversationId=:c ORDER BY receivedAt DESC,insertedAt DESC LIMIT :limit") suspend fun recentMessages(p:String,a:String,c:String,limit:Int):List<ProfessorIncomingEntity>
 @Query("SELECT DISTINCT platform,accountId,conversationId FROM professor_incoming WHERE status IN ('QUEUED','RETRY','PROCESSING')") suspend fun pendingScopes():List<ProfessorScopeRow>
 @Query("SELECT COUNT(*) FROM professor_incoming WHERE status IN ('QUEUED','RETRY','PROCESSING')") fun pendingCount():Flow<Int>
 @Query("UPDATE professor_incoming SET status='COMPLETED',lastError=NULL,updatedAt=:now WHERE platform=:p AND accountId=:a AND conversationId=:c AND messageId=:id AND status='PROCESSING'") suspend fun complete(p:String,a:String,c:String,id:String,now:Long):Int
 @Query("UPDATE professor_incoming SET status='RETRY',attempts=attempts+1,nextRetryAt=:retryAt,lastError=:error,updatedAt=:now WHERE platform=:p AND accountId=:a AND conversationId=:c AND messageId=:id AND status='PROCESSING'") suspend fun retry(p:String,a:String,c:String,id:String,retryAt:Long,error:String?,now:Long):Int
 @Query("UPDATE professor_incoming SET status='FAILED',attempts=attempts+1,lastError=:error,updatedAt=:now WHERE platform=:p AND accountId=:a AND conversationId=:c AND messageId=:id AND status='PROCESSING'") suspend fun fail(p:String,a:String,c:String,id:String,error:String?,now:Long):Int
 @Query("UPDATE professor_incoming SET status='QUEUED',nextRetryAt=0,updatedAt=:now WHERE status='PROCESSING'") suspend fun recover(now:Long):Int
 @Query("UPDATE professor_incoming SET status='CANCELLED',updatedAt=:now WHERE platform=:p AND accountId=:a AND conversationId=:c AND status IN ('QUEUED','RETRY')") suspend fun cancel(p:String,a:String,c:String,now:Long):Int
 @Query("DELETE FROM professor_incoming WHERE platform=:p AND accountId=:a AND conversationId=:c") suspend fun clearIncoming(p:String,a:String,c:String)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertDraft(draft:ProfessorDraftEntity)
 @Query("SELECT * FROM professor_drafts WHERE draftId=:id") suspend fun getDraft(id:String):ProfessorDraftEntity?
 @Query("SELECT * FROM professor_drafts WHERE platform=:p AND accountId=:a AND conversationId=:c ORDER BY updatedAt DESC") fun draftsFlow(p:String,a:String,c:String):Flow<List<ProfessorDraftEntity>>
 @Query("SELECT * FROM professor_drafts WHERE status IN ('PENDING','READY','NEEDS_REVIEW') ORDER BY updatedAt DESC") fun activeDrafts():Flow<List<ProfessorDraftEntity>>
 @Query("UPDATE professor_drafts SET status=:status,updatedAt=:now WHERE draftId=:id") suspend fun updateDraftStatus(id:String,status:String,now:Long):Int
 @Query("DELETE FROM professor_drafts WHERE draftId=:id") suspend fun deleteDraft(id:String)
 @Query("DELETE FROM professor_drafts WHERE platform=:p AND accountId=:a AND conversationId=:c") suspend fun clearDrafts(p:String,a:String,c:String)

 @Insert(onConflict = OnConflictStrategy.ABORT)
 suspend fun insertTrainingTurn(turn: ProfessorTrainingTurnEntity)

 @Query("SELECT * FROM professor_training_turns ORDER BY createdAt, turnId")
 fun trainingTurnsFlow(): Flow<List<ProfessorTrainingTurnEntity>>
 @Query("SELECT * FROM professor_training_turns ORDER BY createdAt DESC, turnId DESC LIMIT 32")
 suspend fun recentTrainingTurns(): List<ProfessorTrainingTurnEntity>
 @Query("SELECT * FROM professor_training_turns WHERE (scopePlatform IS NULL AND accountId IS NULL AND conversationId IS NULL) OR (scopePlatform=:platform AND accountId=:accountId AND conversationId=:conversationId) ORDER BY createdAt DESC, turnId DESC LIMIT 32")
 suspend fun recentTrainingTurnsForScope(platform: String?, accountId: String?, conversationId: String?): List<ProfessorTrainingTurnEntity>

 @Query("DELETE FROM professor_training_turns")
 suspend fun clearTrainingTurns()

 @Insert(onConflict=OnConflictStrategy.ABORT)
 suspend fun insertTrainingLesson(lesson: ProfessorTrainingLessonEntity)

 @Insert(onConflict = OnConflictStrategy.ABORT)
 suspend fun insertTrainingAudit(event: ProfessorTrainingAuditEntity)

 @Query("SELECT * FROM professor_training_audit WHERE lessonId=:lessonId ORDER BY occurredAt DESC, auditId DESC")
 fun trainingAuditFlow(lessonId: String): Flow<List<ProfessorTrainingAuditEntity>>

 @Query("SELECT * FROM professor_training_audit ORDER BY occurredAt DESC, auditId DESC")
 fun trainingAuditFlowAll(): Flow<List<ProfessorTrainingAuditEntity>>

 @Transaction
 suspend fun approveTrainingLessonWithAudit(id: String, now: Long): Int {
  val before = trainingLesson(id) ?: return 0
  val changed = approveTrainingLesson(id, now)
  if (changed == 1) insertTrainingAudit(ProfessorTrainingAuditEntity(UUID.randomUUID().toString(), id, "APPROVED", before.revision, before.rule, before.rule, now))
  return changed
 }

 @Transaction
 suspend fun updateTrainingLessonStateWithAudit(id: String, state: String, now: Long): Int {
  val before = trainingLesson(id) ?: return 0
  val changed = updateTrainingLessonState(id, state, now)
  if (changed == 1) insertTrainingAudit(ProfessorTrainingAuditEntity(UUID.randomUUID().toString(), id, state, before.revision, before.rule, before.rule, now))
  return changed
 }

 @Transaction
 suspend fun editTrainingLessonWithAudit(id: String, rule: String, now: Long): Int {
  val before = trainingLesson(id) ?: return 0
  if (before.state == "REJECTED") return 0
  val changed = editTrainingLesson(id, rule, now)
  if (changed == 1) insertTrainingAudit(ProfessorTrainingAuditEntity(UUID.randomUUID().toString(), id, "EDITED", before.revision + 1, before.rule, rule, now))
  return changed
 }

 @Transaction
 suspend fun deleteTrainingLessonWithAudit(id: String, now: Long) {
  val before = trainingLesson(id) ?: return
  insertTrainingAudit(ProfessorTrainingAuditEntity(UUID.randomUUID().toString(), id, "DELETED", before.revision, before.rule, "", now))
  deleteTrainingLesson(id)
 }

 @Transaction
 suspend fun clearTrainingLessonsWithAudit(now: Long) {
  trainingLessonsSnapshot().forEach { lesson ->
   insertTrainingAudit(ProfessorTrainingAuditEntity(UUID.randomUUID().toString(), lesson.lessonId, "RESET", lesson.revision, lesson.rule, "", now))
  }
  clearTrainingLessons()
 }

 @Query("SELECT * FROM professor_training_lessons")
 suspend fun trainingLessonsSnapshot(): List<ProfessorTrainingLessonEntity>

 @Query("SELECT * FROM professor_training_lessons ORDER BY updatedAt DESC")
 fun trainingLessonsFlow(): Flow<List<ProfessorTrainingLessonEntity>>

 @Query("SELECT * FROM professor_training_lessons WHERE lessonId=:id")
 suspend fun trainingLesson(id: String): ProfessorTrainingLessonEntity?

 @Query("SELECT * FROM professor_training_lessons WHERE state='APPROVED' AND ((scopePlatform IS NULL AND accountId IS NULL AND conversationId IS NULL) OR (scopePlatform=:platform AND accountId=:accountId AND conversationId=:conversationId)) ORDER BY updatedAt DESC")
 suspend fun approvedTrainingLessonsForScope(platform: String, accountId: String, conversationId: String): List<ProfessorTrainingLessonEntity>

 @Query("UPDATE professor_training_lessons SET state='APPROVED', approvedAt=:now, updatedAt=:now WHERE lessonId=:id AND state='PROPOSED'")
 suspend fun approveTrainingLesson(id: String, now: Long): Int

 @Query("UPDATE professor_training_lessons SET state=:state, updatedAt=:now WHERE lessonId=:id AND state IN ('PROPOSED','APPROVED','PAUSED')")
 suspend fun updateTrainingLessonState(id: String, state: String, now: Long): Int

 @Query("UPDATE professor_training_lessons SET rule=:rule, revision=revision+1, updatedAt=:now, state='PROPOSED', approvedAt=NULL WHERE lessonId=:id")
 suspend fun editTrainingLesson(id: String, rule: String, now: Long): Int

 @Query("DELETE FROM professor_training_lessons WHERE lessonId=:id")
 suspend fun deleteTrainingLesson(id: String)

 @Query("DELETE FROM professor_training_lessons")
 suspend fun clearTrainingLessons()
}
