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
- [x] CI runs #27–#30 passed.
- [ ] Device migration and install testing.

## Phase 2 — AI Orchestration (in progress)
- [x] Review-only processor to claim scoped messages, request scope-aware context, generate via AndroidHarness AI adapter, persist review drafts and complete queue items.
- [x] Processing failures use retry policy; coroutine cancellation is preserved.
- [x] Context must end with exact claimed incoming message.
- [x] Drafts are NEEDS_REVIEW with LOW_CONFIDENCE; provider confidence is not calibrated.
- [ ] Inject into app lifecycle/worker and connect real context provider.
- [ ] Orchestration tests and CI validation.

## Phase 3 — Notification Capture (started)
- [x] Pure package classifier/notification text normalizer for supported messaging apps.
- [x] Unit tests for supported packages, unknown packages, empty content, trimming and duplicate title.
- [ ] Android NotificationListenerService, manifest registration, notification access onboarding.
- [ ] Redaction/metadata handling, stable notification identity and update/dismiss lifecycle.
- [ ] Scope/account/contact mapping and durable queue integration (must not guess identity).

## Next phases
- Persistent, per-contact memories and owner-consent workflow.
- Background reliability (worker scheduling, boot recovery, permission/battery diagnostics).
- Platform integration/capability verification for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger and Instagram DM.
- Advanced intelligence/safety: context merging, conversation-ending detection, privacy checks, attachment/document/voice handling, rate limiting.
- Compose Professor settings/dashboard (postponed).
- Physical-device verification and APK install testing.

## Safety baseline
No platform sending is enabled. Orchestrator outputs review-only drafts; sending stays disabled until adapters, recipient verification, privacy/rate checks, sent-state verification and device testing are implemented and validated.
