# Professor Integration Progress

Last updated: 2026-11-23

## Completed and committed on `professor/integration`
- [x] Confirmed architecture decision: AndroidHarness remains the sole AI engine; Professor will call its existing provider/router/local/offline/fallback pipeline and will not introduce a second AI/provider stack.
- [x] Isolated feature branch from imported upstream.
- [x] Baseline/risk audit.
- [x] Product implementation plan.
- [x] Domain model for platform/account/conversation scoping, modes, draft state, adapter capabilities and safety risk flags.
- [x] Fail-closed send gate requiring enabled send mode/platform, ready draft, verified recipient, privacy/rate checks, adapter send and sent-state verification, confidence threshold, and no unresolved risk flags.
- [x] Sensitive-memory consent policy; ordinary memory and sensitive memory have different save rules.
- [x] Thread-isolated in-memory queue with FIFO ordering, duplicate protection, completion tracking and pause behavior.
- [x] Unit tests written for send gate, memory consent, account/platform scope isolation, queue FIFO, dedup, pause and clear-scope behavior.
- [x] CI workflow for APK build + unit tests added; jobs are split so unit tests can report independently of APK/NDK setup. CI setup has been iteratively corrected; latest run status must be checked separately.

## Currently in progress
- CI run #14 is still in progress at the last check; its unit tests are green. Run #13 is the verified full successful build.
- Phase 1 source mapping: initial integration seams documented in `AI_CORE_INTEGRATION_MAP.md`. Confirmed existing AgentEngine, LLM provider abstraction/HarnessProvider, fallback policy/provider, LocalModelProvider, DataStore preferences, Room-backed owner/project memory, MainActivity, and AppNav. Detailed route/wiring review remains.
- [x] Added `ProfessorAiAdapter.kt` (commit `386a8b41`): streams text from an injected AndroidHarness `LlmProvider`, uses `ProviderConfig`/`RequestOptions`, accepts scoped conversation turns, and does not instantiate providers or own API keys. Provider failures are surfaced and empty replies rejected.
- [ ] Run CI against the adapter commit and review any build/test failures.


## Not yet implemented/verified
- Android notification capture service and permission onboarding.
- Persistent queue/drafts/contact memories (Room integration).
- Professor orchestrator connected to AndroidHarness LLM/provider/fallback.
- Compose control dashboard.
- Messaging adapters and real-device tests for WhatsApp, WhatsApp Business, Messenger, Meta Business Messenger and Instagram.
- APK build success, installation and device testing (CI debug build success is verified, but installation/device tests remain).

## Important
This branch currently contains scaffolding and test intent, not a finished messaging agent. No platform sending has been enabled.
