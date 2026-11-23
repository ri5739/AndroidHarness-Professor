# AndroidHarness AI Core Integration Map

Branch: `professor/integration`  
Date: 2026-11-23

## Confirmed seams

- `agent/AgentEngine.kt`: large existing agent execution engine. This is the core agent runtime, not a Professor module. Avoid embedding messaging policy here unless a minimal integration hook is required.
- `llm/HarnessProvider.kt`: existing Harness provider for OpenAI-compatible/free relay routing and model catalog/pool. It is a provider, not a general-purpose AI service facade.
- `llm/FallbackProvider.kt` + `llm/FallbackPolicy.kt`: existing retry/fallback routing and run-owned policy. Professor should inherit this routing rather than implement another provider chain.
- `local/LocalModelProvider.kt`: existing offline inference provider. It calls `LocalModelManager.generateChat`; errors are surfaced as failure events.
- `data/SettingsRepository.kt`: app preferences backed by Android DataStore Preferences (`settingsStore`).
- `data/MemoryRepository.kt`: existing Room-based memory feature, but owner/project memory semantics are not equivalent to contact-scoped Professor memory. Do not mix scopes or reuse without isolation guarantees.
- `ui/AppNav.kt`: app navigation entry point (requires deeper route review before adding Professor screen).
- `MainActivity.kt`: main Compose host, not the correct place for message capture/processing policy.

## Integration direction

1. Build Professor's AI request adapter against the existing provider abstraction (`LlmProvider`, `ProviderConfig`, `RequestOptions`, `StreamEvent`) and existing provider/router construction. Keep provider/model configuration in AndroidHarness.
2. Store Professor preferences separately but use the existing DataStore conventions; do not alter unrelated app settings.
3. Persist message queues, drafts and contact memories in new Room tables with mandatory platform/account/conversation scope. Existing generic memory storage is not contact-memory storage.
4. Treat Android notification capture as a separate capability/service with explicit permission/onboarding and capture limits. No notification listener service was confirmed in the files reviewed yet.
5. Review navigation route conventions before adding a Compose dashboard.
6. Keep platform sending disabled until a platform adapter and send-state verification are implemented and device-tested.

## Files reviewed

- `agent/AgentEngine.kt`
- `llm/HarnessProvider.kt`
- `llm/FallbackProvider.kt`
- `llm/FallbackPolicy.kt`
- `local/LocalModelProvider.kt`
- `data/SettingsRepository.kt`
- `data/MemoryRepository.kt`
- `MainActivity.kt`
- `ui/AppNav.kt` (identified; detailed route implementation still to inspect)

## Limits of this review

This is a source-level mapping pass. It does not establish real notification capture, official API access, platform message sending, or device-level reliability.
