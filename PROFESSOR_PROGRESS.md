# Professor Integration Progress

Last updated: 2026-11-23

## Completed
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

## Phase 1 validation
- [x] CI run #25 passed before the most recent persistence wiring changes.
- [ ] CI rerun after the latest AppDatabase DAO/AppContainer changes.
- [ ] Real-device migration, queue persistence, duplicate handling and recovery test.

## Not implemented / next phases
- Notification capture service and permission onboarding.
- Persistent contact memories.
- Professor orchestrator connected to the AI adapter and AndroidHarness routing/fallback.
- Compose settings/dashboard.
- Platform adapters and testing for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger and Instagram.
- Physical-device verification and APK install testing.

## Safety
No platform sending is enabled. Draft-only remains the intended safe default until adapter capability, recipient verification, privacy/rate limits, sent-state verification and device tests pass.
