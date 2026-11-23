# Professor Integration Progress

Last updated: 2026-11-23

## Verified repository state
- Working branch: `professor/integration`
- Latest verified CI commit: `abb3095b637276208683d88bfbc881ef6fe1faf2`; newer policy/test commits are present but not yet CI-verified.
- Latest verified CI: [run #89](https://github.com/ri5739/AndroidHarness-Professor/actions/runs/45216270264) — completed successfully (unit tests + debug APK).
- CI jobs verified: JVM unit tests and debug APK build both succeeded.
- This CI result verifies the current branch build/tests only; it does not prove real-device messaging behavior or all roadmap milestones.

## Phase 1 — Foundation
- [x] AndroidHarness remains the single AI core; Professor does not create a parallel provider/API-key stack.
- [x] Dedicated integration branch, audit and implementation plan.
- [x] Scoped domain models, send safety gate and contact-memory consent policy.
- [x] Per-conversation queue and dedup tests.
- [x] CI workflow for unit tests and debug APK build.
- [x] Professor AI adapter streams from injected AndroidHarness provider.
- [x] Room entities, deduplicating enqueue, per-conversation claim and status transitions.
- [x] Retry/recovery/cancel APIs; bounded exponential retry policy.
- [x] Draft persistence and review status APIs.
- [x] Room migration 27→28 and AppContainer exposure.
- [ ] Device migration/install verification.

## Phase 2 — AI Orchestration
- [x] Review-only processor: scoped context, AI draft generation, persistence, queue completion.
- [x] Context provider and mismatch validation.
- [x] Context window bounded, chronological, scope-isolated, with claimed message last.
- [x] Retry policy and cancellation propagation.
- [x] Orchestration unit tests.
- [x] CI validation — run #89 passed both unit-tests and debug-apk jobs (at commit `abb3095`; later commits require a fresh CI run).
- [ ] Full bidirectional conversation history. Current platform notification capture supplies incoming-only messages; outgoing turns must come from a reliable, consented source before this can be implemented. Do not fabricate assistant/outgoing turns.
- **Status:** Core review-only orchestration is implemented and CI-verified. Phase 2 is not fully complete while bidirectional history remains a requirement.

## Phase 3 — Notification Capture
- [x] Supported package classifier and text normalizer for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger, Instagram.
- [x] NotificationListenerService registered in Manifest and durable queue scheduling.
- [x] Safe capture of notification title/text with text normalization.
- [x] Notification identity helper: package-scoped account bucket and notification-key-first conversation IDs, hashed for privacy.
- [x] Unit tests for notification identity and package scoping; covered by successful CI run #75.
- [ ] Notification access onboarding and status UI.
- [ ] Notification metadata redaction, notification update/dismiss lifecycle review.
- [ ] Real account/contact identity mapping (not reliably exposed by notification metadata; do not guess).

## Phase 4 — Background processing/recovery
- [x] Unique scoped WorkManager jobs, bounded draining.
- [x] Startup recovery for interrupted queue entries and re-enqueue of pending scopes.
- [x] Retry for unavailable AI provider/config/key and recoverable processing errors.
- [ ] Battery/background behavior and real-device verification.

## Remaining milestones — next implementation batch
1. Add notification-access onboarding and live permission/status UI.
2. Review notification update/dismiss semantics and redact sensitive notification metadata before persistence/logging.
3. Implement draft management UI with owner edit/review/cancel controls; keep send disabled.
4. Implement Smart Reply Revision Option C and deterministic handling of overlapping messages.
5. Add explicit account/contact mapping; retain conservative package-scoped account buckets until the owner maps them.
6. Add contact-memory persistence with consent/review/delete controls and strict platform/account isolation.
7. Add privacy checks, rate limiting, attachment/document/OCR/voice capability handling, and a platform capability matrix.
8. Add settings, audit log, emergency pause, and battery/background instrumentation.
9. Run migration/install and real-device tests; only claim platform support after per-platform evidence.

## Updated reply and approval policy (owner requirement, 2026-11-23)
- Ordinary, low-risk conversation replies should be eligible for automatic sending once the platform adapter and release gates are verified; examples include greetings, everyday conversation, and routine non-sensitive questions.
- Owner approval is required only when the proposed outgoing reply would disclose confidential/private information, such as passwords, OTPs, banking/payment credentials, private documents, personal identifiers, private contact details, or confidential business/personal records.
- For a mixed message, the agent may answer the ordinary portion while withholding the sensitive portion. It should say it will check with the owner before sharing that information, then continue the safe conversation naturally.
- A sensitive incoming request does not by itself make every possible reply sensitive: a safe acknowledgement such as “I’ll confirm with the owner before sharing that” may be sent automatically if it contains no protected information.
- Risk detection must inspect the proposed outgoing text, not only the incoming notification. Uncertain classification must fail closed for disclosure: do not reveal the information; use a safe acknowledgement and request owner approval.
- [x] Added `ProfessorOutgoingReplyPolicy` to classify generated outgoing text for OTP/password/API-key/payment-secret, private contact, financial, and explicitly confidential details; ordinary text is marked auto-send-eligible, sensitive text requires owner approval, and a safe Bengali acknowledgement is provided.
- [x] Integrated outgoing privacy assessment into orchestration so generated drafts containing detected private information receive `SENSITIVE_DATA`; added unit tests for ordinary, sensitive, mixed, and empty replies.
- [ ] Still required before this policy is active end-to-end: owner approval request/response UI and persistence, safe mixed-reply regeneration/continuation, platform send adapters, and successful CI/device verification.
- Never send passwords, OTPs, payment credentials, authentication tokens, private keys, or similarly high-risk secrets automatically, even if the sender pressures the agent.

## Current implementation boundary
No platform sending is enabled yet. The current orchestrator still creates review-only drafts, and the platform-specific send adapters, owner approval workflow, outgoing-content privacy classifier, sent-state verification, and physical-device validation remain to be implemented. Do not describe automatic replies as active until those components and release gates are verified.


## Owner-guided Training Chat — newly requested feature (2026-11-23)
- **Status: Not implemented yet.** This requirement is now explicitly recorded in the repository so it is not lost between phases.
- Add a dedicated in-app Training Chat where the owner teaches Agent Professor through natural conversation, examples, and corrections.
- The agent must preview proposed lessons and require the owner's explicit confirmation before saving/activating durable rules.
- Persist approved lessons with global/platform/account/contact scopes; strictly isolate scopes and provide inspect/edit/pause/delete/reset controls.
- Feed only relevant active lessons into the existing Professor orchestrator via AndroidHarness AI Core. Do not create a parallel AI/provider/API-key stack.
- Reject/redact secrets; learned rules must never override confidentiality approval, safety gates, platform capability checks, or emergency pause.
- Include conflict resolution and a no-send simulation/test mode.
- Full requirements and delivery checklist: `AGENT_TRAINING_CHAT_SPEC.md`.
- [ ] Training Chat UI, lesson extraction/approval, persistence, retrieval integration, management controls, tests, and device verification remain to be implemented.


## Expanded owner-requested implementation batch (started 2026-11-23)
Current branch head: `a452ee55920fc1781f8c73827b484bf7f8766724`.
Latest verified CI remains run #89 at `abb3095b637276208683d88bfbc881ef6fe1faf2`; it predates the two new commits below. Do not treat the new code as CI-verified yet.

### Initial domain foundation committed
- [x] Added explicit owner-confirmed account/contact mapping model; it refuses use across mismatched platform/account/conversation scopes.
- [x] Added deterministic sliding-window rate limiter keyed by full conversation scope.
- [x] Added bidirectional conversation-turn record/merge contract with chronological ordering, deduplication and scope isolation. Outgoing turns must come from a reliable source; they are never fabricated from incoming notifications.
- [x] Added fail-closed attachment classification for image/PDF/document/audio/video/unknown; it does not claim OCR/transcription/extraction until adapter capability is verified.
- [x] Added metadata-only audit event model (message/attachment content intentionally excluded).
- [x] Added proposed/approved Training Lesson domain state; activation is explicit and credential-like lessons are rejected.
- [x] Added JVM tests for these domain rules.
- [ ] Persist these new models and wire them to app UI/orchestrator.
- [ ] Add notification permission/onboarding screen and draft management screen.
- [ ] Implement Training Chat UI, AI lesson extraction through AndroidHarness AI Core, persistence, approval controls and retrieval.
- [ ] Implement Smart Reply Revision Option C after verifying its exact previously agreed behavior; do not guess semantics.
- [ ] Add real outgoing conversation history source per platform.
- [ ] Implement actual attachment extraction/OCR/document/voice adapters only where capabilities are supported.
- [ ] Add durable audit log, emergency pause and platform capability verification.
- [ ] Add battery/background instrumentation, automated/instrumentation tests and real-device validation.
- [ ] Security audit and release gates.

### Commits in this batch
- `00cb932f202a035d2714bc3d682cf7f6de69acf3` — domain policies.
- `a452ee55920fc1781f8c73827b484bf7f8766724` — policy tests.

**Important:** The two commits above have not yet been covered by a new Actions run. GitHub Actions run #89 is green for its earlier commit only. No app-level Training Chat UI, durable lesson store, draft UI, platform send adapter, OCR/voice adapter, or real-device verification is being claimed complete.
