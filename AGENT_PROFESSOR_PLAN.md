# Agent Professor Implementation Plan

## Current progress (verified on 2026-11-23)
- [x] Dedicated integration branch created from imported source.
- [x] AndroidHarness source imported; architecture/risk plan committed.
- [x] Domain scaffold: platforms, scoped account/conversation IDs, drafts, safety gate, sensitive-memory consent.
- [x] Queue/persistence scaffold (Room): enqueue, claim, complete, retry, fail, recovery, cancel, draft persistence.
- [x] Notification package classifier for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger, Instagram.
- [x] Orchestrator review-only flow and AI adapter seam.
- [x] JVM tests and CI workflow present.
- [x] Latest CI run #41 passed (unit tests + debug APK build): https://github.com/ri5739/AndroidHarness-Professor/actions/runs/45148488040
- [x] Phase 2 provider/router/configuration is wired to AndroidHarness AI Core; persisted incoming-message context window integrated. Full bidirectional history requires platform-specific conversation capture.
- [ ] Phase 3: implement notification listener service, user permission/setup, safe notification parsing and durable queue mapping.
- [ ] Phase 4: background worker/lifecycle integration and interrupted-processing recovery verification.
- [ ] Later phases: draft management UI, contact memory, safety review, platform-specific interaction/send adapters, device tests, dashboard polish.

## Parallel execution policy
Work continues on independent tasks when a phase is blocked. Never bypass a dependency or silently mark a blocked task complete. Keep integration checkpoints and run tests after merged changes. If two tasks touch the same files, serialize them to avoid conflicts.

## Product rule
AndroidHarness AI Core remains the sole AI brain. Agent Professor is orchestration, policy, messaging, and memory and must call existing AI Core routing. Do not add an independent model stack.

## User-facing controls
- Master enable/disable and emergency stop.
- Per-platform and per-account enablement.
- Draft-only vs send-enabled mode (draft-only is default).
- Online/offline routing inherited from AndroidHarness.
- Smart delay, confidence threshold, quiet hours, rate limits.
- Contact-specific pause/takeover and memory controls.
- Queue, draft, task, and audit log views.
- Review/edit/delete memories; sensitive-memory consent prompt.
- Platform permission/setup status.

## Data isolation keys
All conversation data must be keyed by platform + account identity + conversation/contact ID. Never share contact memories between accounts/platforms. Owner Personal Knowledge Base stays separate.

## Safety gates before send
1. Verify target account and conversation identity.
2. Merge incoming context safely; queue per conversation and deduplicate.
3. Draft and check confidence/privacy/attachment/link risk.
4. Allow cancel/edit and human takeover.
5. Respect rate limits, retry with backoff, and inspect send state.
6. Prevent retry duplicate sends; log the result.
7. Global pause/emergency stop overrides every queue and adapter.

## Platform capability matrix
Adapters must report capabilities rather than assume all platforms behave alike: read incoming messages, extract notifications, open conversation, draft, send, verify send state, attachments, reactions, official API eligibility. Unsupported actions stay disabled and are never faked.

## Release gates
- Kotlin unit tests for policy, dedupe, context merge, retries, rate limiting, contact isolation, and sensitive-memory consent.
- Instrumentation tests for permission/setup and UI.
- CI debug APK build; actual-device testing of each enabled adapter.
- No auto-send default until safety tests pass and owner explicitly enables it.
