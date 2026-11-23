# Professor Integration Progress

Last updated: 2026-11-23

## Completed and committed on `professor/integration`
- [x] Isolated feature branch from imported upstream.
- [x] Baseline/risk audit.
- [x] Product implementation plan.
- [x] Domain model for platform/account/conversation scoping, modes, draft state, adapter capabilities and safety risk flags.
- [x] Fail-closed send gate requiring enabled send mode/platform, ready draft, verified recipient, privacy/rate checks, adapter send and sent-state verification, confidence threshold, and no unresolved risk flags.
- [x] Sensitive-memory consent policy; ordinary memory and sensitive memory have different save rules.
- [x] Thread-isolated in-memory queue with FIFO ordering, duplicate protection, completion tracking and pause behavior.
- [x] Unit tests written for send gate, memory consent, account/platform scope isolation, queue FIFO, dedup, pause and clear-scope behavior.
- [x] CI workflow for APK build + unit tests added; jobs are split so unit tests can report independently of APK/NDK setup. CI setup has been iteratively corrected; latest run status must be checked separately.

## Not yet implemented/verified
- Android notification capture service and permission onboarding.
- Persistent queue/drafts/contact memories (Room integration).
- Professor orchestrator connected to AndroidHarness LLM/provider/fallback.
- Compose control dashboard.
- Messaging adapters and real-device tests for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger and Instagram.
- APK build success, installation and device testing.

## Important
This branch currently contains scaffolding and test intent, not a finished messaging agent. No platform sending has been enabled.
