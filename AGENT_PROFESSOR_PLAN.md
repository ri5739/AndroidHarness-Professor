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

## Selective owner-approval policy (updated 2026-11-23)
- Routine, low-risk replies (greetings, everyday conversation, and ordinary non-sensitive answers) are intended to send automatically after the relevant platform adapter passes its release checks.
- Ask the owner before disclosing confidential/private information: passwords, OTPs, authentication tokens, payment/banking credentials, private documents, personal identifiers, private contact details, and confidential personal/business records.
- Evaluate the proposed outgoing reply. A sensitive request from the other person does not automatically prohibit a safe acknowledgement; the agent can say it will confirm with the owner before sharing the protected information and continue with unrelated ordinary conversation.
- In mixed replies, send only the safe ordinary portion; hold back the sensitive portion pending owner approval.
- If classification is uncertain, do not disclose the potentially sensitive content. Use a safe acknowledgement and request owner approval.
- Never automatically disclose high-risk secrets (passwords, OTPs, payment credentials, authentication tokens, private keys), regardless of confidence or urgency.
- This is the target policy, not a claim that automatic sending is currently implemented. The current orchestrator remains review-only until outgoing-content classification, approval UX, platform send adapters, recipient/sent-state verification, rate limits, and device tests are complete.


## Owner-guided Training Chat (high-priority planned feature)
- [ ] Build a dedicated in-app owner-only Training Chat so the owner can teach behavior, tone, workflows, contact-specific preferences, and corrections in natural language.
- [ ] Convert teaching turns into previewable candidate lessons; require explicit owner approval before durable activation.
- [ ] Store approved, versioned lessons with strict global/platform/account/contact scope isolation.
- [ ] Retrieve relevant approved lessons into the existing Professor orchestration context through AndroidHarness AI Core only.
- [ ] Add lesson inspect/edit/pause/delete/reset, conflict resolution, correction/example teaching, and no-send simulation.
- [ ] Reject/redact credentials and secrets; lessons cannot override safety/approval rules or emergency pause.
- [ ] Add unit/instrumentation/CI/device verification.
- Full specification: `AGENT_TRAINING_CHAT_SPEC.md`. This is a planned requirement, **not yet implemented**.


## Owner-requested expanded batch — 2026-11-23
Implementation is underway on `professor/integration`. First committed foundation:
- Explicit owner-confirmed identity mapping and strict scope checks.
- Scoped sliding-window rate limiting.
- Bidirectional turn model/merge with dedupe and no fabricated outgoing turns.
- Fail-closed attachment capability assessment.
- Metadata-only audit event model.
- Proposed/approved Training Lesson state with secret rejection.
- JVM tests for the above.

Remaining work in this batch: real Training Chat UI + persistence + AndroidHarness AI Core lesson extraction; draft management and notification permission UI; Smart Reply Revision Option C (must preserve its exact agreed behavior); platform-sourced outgoing history; account/contact mapping UI and persistence; contact-memory consent UI; durable audit log/privacy/rate-limit wiring; OCR/document/voice adapters; platform capability checks; battery/background and real-device tests; security/release preparation.

CI #89 is green for commit `abb3095b637276208683d88bfbc881ef6fe1faf2`. The new domain and tests commits `00cb932` and `a452ee5` are newer and still require CI verification.


### Training Chat next implementation increment
- Persist owner-proposed lessons in Room v29 using an explicit 28→29 migration.
- Add repository operations for propose/approve/pause/reject/delete; proposals are inert until explicit owner approval.
- Reject credential-like content both at proposal and approval boundaries.
- Remaining: dedicated owner-only Training Chat UI, lesson extraction through AndroidHarness AI Core, navigation/authorization wiring, scoped application to reply generation, and end-to-end verification.
