# Agent Professor — In-App Training Chat Specification

Status: **Planned; not implemented yet**  
Priority: **High — preserve as a committed roadmap requirement**  
Date added: 2026-11-23

## Goal

Add a dedicated chat inside Agent Professor where the owner can teach, correct, and configure the agent through natural conversation. This is an owner-facing training interface, separate from chats with external contacts.

## Core experience

- A dedicated **Training Chat** screen in the app.
- The owner can explain desired behavior in natural language, provide examples, correct bad replies, and teach platform/contact-specific preferences.
- The agent acknowledges each proposed lesson and shows a clear preview of what it intends to remember before saving durable rules.
- The owner can inspect, edit, pause, delete, and reset learned rules/preferences at any time.
- The agent uses approved lessons when preparing replies, while respecting platform/account/conversation scope and the existing safety policy.
- Include a way to test a proposed rule against sample messages without sending anything externally.

## Learning and approval model

1. Training conversation is interpreted by AndroidHarness' existing AI Core; do not add a parallel model/provider/API-key stack.
2. Treat statements in Training Chat as proposed lessons, not automatically trusted facts or unrestricted instructions.
3. Convert teachable requests into structured candidate memories/rules with fields such as: rule text, category, scope, examples, created/updated time, and active/paused state.
4. Show the candidate rule to the owner and require explicit confirmation before persisting it as an active durable lesson.
5. Keep platform, account, and contact scopes isolated. A contact-specific lesson must never leak into another contact/account.
6. Never store passwords, OTPs, authentication tokens, payment credentials, private keys, or other secrets as reusable memory. Reject or redact such content.
7. Resolve conflicts by showing the conflicting rules and asking the owner which should take precedence; do not silently overwrite.
8. Support corrections: owner can say a draft was wrong, explain the preferred response, preview the resulting lesson, and approve it.
9. Training lessons can guide tone, style, workflow, and safe response preferences, but cannot override safety gates, owner approval requirements for confidential disclosures, platform capabilities, or emergency pause.

## Suggested architecture

- Training Chat UI and conversation history, owner-only.
- Training session/repository separate from external-message queue.
- Candidate lesson extraction/preview/confirmation flow.
- Persistent, versioned lesson store with explicit scope keys (platform + account + optional contact; global owner rules clearly distinguished).
- Retrieval/selection layer that supplies only relevant active lessons to the existing Professor orchestrator.
- Rule conflict detection, audit history, pause/delete/reset controls.
- Unit tests for scope isolation, secret rejection, approval-before-save, conflict handling, retrieval relevance, and safety-policy precedence.

## Delivery checklist

- [ ] Training Chat screen and owner-only entry point.
- [ ] Training session persistence and conversation history.
- [ ] Candidate lesson extraction using AndroidHarness AI Core.
- [ ] Lesson preview and explicit owner approval before activation.
- [ ] Persistent lesson repository, versioning, and scope isolation.
- [ ] Learned-rule retrieval integrated into reply context.
- [ ] Edit/pause/delete/reset management UI.
- [ ] Correction and example-based teaching flow.
- [ ] Rule conflict resolution UI.
- [ ] Secret detection/redaction and safety precedence tests.
- [ ] Test/simulate mode that never sends external messages.
- [ ] CI, migration, instrumentation, and real-device verification.

## Non-goals / safeguards

- This is not model fine-tuning or training a new foundation model; it is controlled owner-guided instructions/preferences/memory.
- Do not create a second AI provider stack. AndroidHarness remains the sole AI routing/provider/API-key system.
- Do not let external senders teach or modify owner rules through ordinary incoming messages.
- Do not activate a lesson merely because it appeared in the training transcript; owner confirmation is required.
- Training Chat does not enable external auto-send. Existing draft-only/send safety gates remain authoritative.

## Completion definition

The feature is complete only when the owner can teach a rule in-app, review and approve it, see it persisted with the correct scope, observe it applied to a matching test/reply draft, and edit or remove it; automated tests and device verification must pass.
