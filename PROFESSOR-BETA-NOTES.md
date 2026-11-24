# Agent Professor beta notes

This beta adds the first notification-based automatic reply path.

## Reply behavior

- Ordinary incoming messages are eligible for automatic replies.
- Private-information questions about the owner are held for owner approval.
- A short Bengali acknowledgement is sent for a held private-information request when the notification supports inline reply.
- Later ordinary messages in the same conversation continue through the reply worker while a private reply is waiting for approval.
- Held replies can be reviewed, edited, and explicitly approved for sending.

## Important compatibility limits

Automatic sending currently uses Android notification inline-reply actions (`RemoteInput`). It only works when the source messaging app exposes a free-form inline-reply action in its notification. Apps or notification types that do not expose that action will keep the reply in the draft inbox; this beta does not yet include universal, app-specific send adapters.

The notification reply action is kept in process memory. If Android terminates the app process, or the source app expires/cancels the action, an approval send may fail and the draft remains available for review. A successful Android PendingIntent dispatch is not proof of delivery or recipient receipt.

Private-information detection is a conservative phrase-based Bengali/English classifier and may need refinement. Never rely on it as a perfect privacy boundary; review held replies before sending.

## Build verification

The integration CI for commit `eb03e66e3d72cdf5938f7fb9d9a01991b189459f` completed successfully:
- Debug APK build and artifact upload: passed.
- JVM unit tests: passed.
- Android instrumentation tests: skipped because emulator tests are opt-in in this workflow.
