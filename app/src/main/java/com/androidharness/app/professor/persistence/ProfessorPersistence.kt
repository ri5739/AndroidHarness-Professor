package com.androidharness.app.professor.persistence
import com.androidharness.app.professor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProfessorPersistence(private val dao:ProfessorPersistenceDao) {
 private fun p(scope:ConversationScope)=scope.platform.name
 suspend fun enqueue(m:IncomingMessage):Boolean=dao.enqueue(ProfessorIncomingEntity(p(m.scope),m.scope.accountId,m.scope.conversationId,m.messageId,m.senderId,m.text,m.receivedAtEpochMillis))!=-1L
 suspend fun claimNext(scope:ConversationScope,now:Long=System.currentTimeMillis()):IncomingMessage? { val row=dao.claimNext(p(scope),scope.accountId,scope.conversationId,now)?:return null; return IncomingMessage(row.messageId,scope,row.senderId,row.text,row.receivedAt) }
 suspend fun complete(scope:ConversationScope,id:String,now:Long=System.currentTimeMillis())=dao.complete(p(scope),scope.accountId,scope.conversationId,id,now)==1
 suspend fun retry(scope:ConversationScope,id:String,error:String?,now:Long=System.currentTimeMillis()):Boolean { val attempts=(dao.processingAttempts(p(scope),scope.accountId,scope.conversationId,id)?:return false)+1; if(attempts>=ProfessorRetryPolicy.MAX_ATTEMPTS)return dao.fail(p(scope),scope.accountId,scope.conversationId,id,error?.take(500),now)==1; return dao.retry(p(scope),scope.accountId,scope.conversationId,id,ProfessorRetryPolicy.retryAt(now,attempts),error?.take(500),now)==1 }
 suspend fun fail(scope:ConversationScope,id:String,error:String?,now:Long=System.currentTimeMillis())=dao.fail(p(scope),scope.accountId,scope.conversationId,id,error?.take(500),now)==1
 suspend fun recoverInterrupted(now:Long=System.currentTimeMillis())=dao.recover(now)
 suspend fun cancelPending(scope:ConversationScope,now:Long=System.currentTimeMillis())=dao.cancel(p(scope),scope.accountId,scope.conversationId,now)
 suspend fun clearScope(scope:ConversationScope) { dao.clearIncoming(p(scope),scope.accountId,scope.conversationId); dao.clearDrafts(p(scope),scope.accountId,scope.conversationId) }
 fun pendingCountFlow():Flow<Int> = dao.pendingCount()
 fun incomingFlow(scope:ConversationScope):Flow<List<IncomingMessage>> = dao.messagesFlow(p(scope),scope.accountId,scope.conversationId).map { rows->rows.filter { it.status in setOf("QUEUED","RETRY","PROCESSING") }.map { IncomingMessage(it.messageId,scope,it.senderId,it.text,it.receivedAt) } }
 fun draftsFlow(scope:ConversationScope):Flow<List<ReplyDraft>> = dao.draftsFlow(p(scope),scope.accountId,scope.conversationId).map { it.map(::draftFrom) }
 fun activeDraftsFlow():Flow<List<ReplyDraft>> = dao.activeDrafts().map { it.map(::draftFrom) }
 suspend fun saveDraft(d:ReplyDraft,now:Long=System.currentTimeMillis()) { val flags=d.riskFlags.joinToString(prefix="[",postfix="]") { "\"" + it.name + "\"" }; dao.upsertDraft(ProfessorDraftEntity(d.draftId,p(d.scope),d.scope.accountId,d.scope.conversationId,d.inReplyToMessageId,d.text,d.status.name,flags,d.confidence,now,now)) }
 suspend fun draft(id:String):ReplyDraft?=dao.getDraft(id)?.let(::draftFrom)
 suspend fun setDraftStatus(id:String,status:DraftStatus,now:Long=System.currentTimeMillis())=dao.updateDraftStatus(id,status.name,now)==1
 suspend fun deleteDraft(id:String)=dao.deleteDraft(id)
 private fun draftFrom(e:ProfessorDraftEntity):ReplyDraft { val scope=ConversationScope(MessagingPlatform.valueOf(e.platform),e.accountId,e.conversationId); val flags=e.riskFlagsJson.removePrefix("[").removeSuffix("]").split(",").map { it.trim().removePrefix("\"").removeSuffix("\"") }.mapNotNull { name->runCatching { RiskFlag.valueOf(name) }.getOrNull() }.toSet(); return ReplyDraft(e.draftId,scope,e.inReplyToMessageId,e.text,runCatching { DraftStatus.valueOf(e.status) }.getOrDefault(DraftStatus.PENDING),flags,e.confidence) }
 companion object { fun newDraftId()=UUID.randomUUID().toString() }
}
