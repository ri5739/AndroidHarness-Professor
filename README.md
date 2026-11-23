# AndroidHarness-Professor

A planned fork/integration of [AndroidHarness](https://github.com/Sanuu7/AndroidHarness) with Agent Professor capabilities.

## Status

- [x] Target repository verified.
- [ ] Import upstream AndroidHarness source and preserve its MIT license/attribution.
- [ ] Establish a baseline build and tests.
- [ ] Integrate Agent Professor as a modular layer using AndroidHarness AI as the sole AI brain.
- [ ] Add messaging adapters, safeguards, and control center.
- [ ] Run build/tests and publish verified artifacts.

## Architecture principle

**AndroidHarness AI remains the only AI brain.** Agent Professor will add personality, context, isolated contact memory, messaging automation, safety controls, and owner settings around the existing AI engine—not a separate competing AI engine.

## Messaging targets

- WhatsApp personal
- WhatsApp Business
- Messenger personal
- Meta Business Messenger
- Instagram DM

Platform capabilities and access requirements must be verified individually. Do not claim an adapter is operational until it is implemented and tested.

## Safety requirements

- Per-platform and per-contact settings, context and memory isolation.
- Draft review/cancellation, queue deduplication, status checks and bounded retries.
- Human takeover, rate limits, privacy checks, confidence thresholds, and emergency stop.
- Sensitive contact memory requires owner approval.

## Source and licensing

Upstream: https://github.com/Sanuu7/AndroidHarness  
Upstream is MIT licensed; retain the original license and attribution when importing.

## Implementation notes

No upstream source has been imported yet. The initial repository setup is only documentation and is not an app build.
