# Agent Professor Beta

This prerelease packages the current Agent Professor integration into AndroidHarness.

## Included in this build
- Owner-only Training Chat with persisted history, lesson proposals, approval, editing, pause/resume, deletion, reset, scope isolation, conflict handling, audit history, and no-send simulation.
- Notification capture for WhatsApp, WhatsApp Business, Messenger, Meta Business Suite, and Instagram, controlled by independent opt-in switches.
- Notification access status and setup entry point.
- Persisted reply draft inbox with edit, review-state, and delete controls.
- Existing AndroidHarness AI Core integration for Professor reply drafting.
- Bengali-first default language policy for real-message reply generation; Training Chat has its own language behavior.

## Important limitations
- This is a review/draft beta. Platform-specific outgoing send adapters and sent-state verification are not enabled.
- Notification capture depends on Android Notification Access and each platform's notification settings.
- Account/contact identity is conservatively scoped; notification metadata is not proof of account identity.
- OCR, document extraction, and incoming audio transcription are not claimed as complete for Professor.
- The owner should connect each platform, enable its capture switch, and manually verify behavior on a physical device.
