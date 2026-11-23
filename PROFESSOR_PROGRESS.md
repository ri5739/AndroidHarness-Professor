# Professor Integration Progress

Last updated: 2026-11-23

## Phase 1 — Foundation
- [x] AndroidHarness remains the single AI core; Professor does not create a parallel provider/API-key stack.
- [x] Dedicated integration branch, audit and implementation plan.
- [x] Scoped domain models, send safety gate and contact-memory consent policy.
- [x] In-memory per-conversation FIFO/dedup queue and policy tests.
- [x] CI workflow for unit tests and debug APK build.
- [x] ProfessorAiAdapter streams from injected AndroidHarness LlmProvider; no provider/key ownership.
- [x] Persistent Room entities for incoming messages and drafts.
- [x] Deduplicating enqueue with platform/account/conversation/message composite key.
- [x] Per-conversation FIFO claim and processing status transitions.
- [x] Retry/recovery/cancel APIs; bounded exponential retry policy.
- [x] Draft save/load/flow/status/delete APIs.
- [x] Room migration 27→28 creates Professor tables/indexes without altering existing tables.
- [x] Professor DAO exposed from AppDatabase and repository exposed from AppContainer.
- [ ] CI validation of the latest persistence wiring and device migration tests.

## Phase 2 — AI Orchestration (in progress)
- [x] Added review-only processor to claim scoped messages, request scope-aware context, generate via AndroidHarness AI adapter, persist review drafts and complete queue items.
- [x] Processing failures use existing retry policy; coroutine cancellation is preserved.
- [x] Context must end with the exact claimed incoming message to reduce scope/context mismatch risk.
- [x] Drafts are NEEDS_REVIEW with LOW_CONFIDENCE; no calibrated confidence is available in provider stream.
- [ ] Inject into app lifecycle/worker and connect real context provider.
- [ ] Add orchestration tests and validate CI.

## Remaining
- Notification capture service and permission onboarding.
- Persistent, per-contact memories and owner-consent workflow.
- Compose Professor settings/dashboard (master toggle, platform/account scopes, drafts, queues, logs, emergency stop).
- Platform integration/capability verification for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger and Instagram DM.
- Advanced intelligence/safety: context merging, conversation-ending detection, privacy checks, attachment/document/voice handling, rate limiting.
- Background reliability (worker scheduling, boot recovery, permission/battery diagnostics).
- Physical-device verification and APK install testing.


## Safety baseline
No platform sending is enabled. Orchestrator outputs review-only drafts; sending stays disabled until adapters, recipient verification, privacy/rate checks, sent-state verification and device testing are implemented and validated.
