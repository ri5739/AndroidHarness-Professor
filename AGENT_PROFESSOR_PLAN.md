# Agent Professor Implementation Plan

## Product rule
AndroidHarness AI Core remains the sole AI brain. Agent Professor is an orchestration, policy, messaging, and memory feature that calls the existing AI Core. Do not add an independent model stack.

## User-facing controls
- Master enable/disable and emergency stop.
- Per-platform and per-account enablement.
- Draft-only vs send-enabled mode (draft-only is default).
- Online/offline routing inherited from AndroidHarness, with offline fallback.
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
Adapters must report capabilities rather than assume all platforms behave alike: read incoming messages, extract notifications, open conversation, draft, send, verify sent state, attachments, reactions, official API eligibility. Unsupported actions stay disabled and are never faked.

## Release gates
- Kotlin unit tests for policy, dedupe, context merge, retries, rate limiting, contact isolation, and sensitive memory consent.
- Instrumentation tests for permission/setup and UI.
- CI debug APK build; actual-device testing of each enabled adapter.
- No auto-send default until safety tests pass and owner explicitly enables it.
