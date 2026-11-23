# AndroidHarness-Professor Integration Audit

Branch: `professor/integration`
Base: `import/androidharness-source`
Audit date: 2026-11-23

## Verified upstream baseline
- AndroidHarness is a native Kotlin + Jetpack Compose Android application.
- App namespace/application ID baseline: `com.androidharness.app`.
- Main source package: `app/src/main/java/com/androidharness/app`.
- Android min SDK 26, compile SDK 37, target SDK 36 (from app Gradle config).
- Existing app already has agent runtime, LLM provider/fallback, persistence/data, automation, notifications, local/offline models and Compose UI.
- Existing key code areas include `agent/AgentEngine.kt`, `llm/`, `data/`, `automation/`, `ui/`, and `local/`.
- Existing app permissions include internet/network, notifications, foreground service, wake lock, audio, and storage. No messaging notification listener/accessibility integration was confirmed in this initial audit.
- Import status points to upstream commit `c2f12baf12add491f21f1b2f0d113fca0adf8b05`.

## Devil's Advocate risks / design constraints
1. Do not create a second AI engine or competing model. Professor must use AndroidHarness's existing provider/router/fallback interfaces.
2. Do not wire auto-send before explicit platform capability checks, permission flows, drafts, pause/kill switch, and duplicate-send protections exist.
3. WhatsApp/Messenger/Instagram support cannot be promised as universal direct API access. Android notification/accessibility adapters and official Meta API eligibility have different capabilities and policy limits.
4. Existing app is large and native; broad UI edits before understanding navigation/state/DI can break unrelated features.
5. A green import workflow only proves the copy operation succeeded; it does not prove compilation, tests, or APK signing/build.
6. Keep all integration changes on this feature branch; do not replace or force-push upstream source or default branch.

## Integration direction
- Professor is a feature package within AndroidHarness, not a separate app/model.
- One orchestrator; adapters per platform/account; per-conversation queue; drafts first.
- Persistence entities scoped by platform + account + conversation/contact.
- Start with safe notification/draft capability; sending is gated and opt-in per platform.
- Prefer existing provider router/fallback and existing Room/DataStore patterns.

## Work phases
- P0: baseline audit and branch isolation (started; branch created).
- P1: map AndroidHarness provider, settings, persistence, navigation, notification/service hooks.
- P2: Professor domain model, config, per-platform/account/contact scope, draft/queue state, tests.
- P3: safe message ingestion + notification listener/permission onboarding; supported platform capability matrix.
- P4: AI orchestration via existing AndroidHarness AI Core; language/personality/context policies.
- P5: drafts, smart delay/revision, queue/dedup/retry/status/rate limits, pause/takeover/emergency stop.
- P6: contact memory with consent gates for sensitive data; privacy filters; attachment/OCR/transcription safety.
- P7: Professor control UI, logs, queue/drafts/memory review.
- P8: platform adapters in order of feasible testability; WhatsApp personal/Business, Messenger personal/Business, Instagram.
- P9: automated tests, CI build, device testing; only claim a platform works after real test evidence.

## Immediate next
Audit navigation, Room/DataStore schemas, AI provider invocation, and notification/service code. Produce a dependency map before changing runtime behavior.
