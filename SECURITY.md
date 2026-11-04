# Security

## Reporting a vulnerability

Please report privately via **GitHub → Security → Report a vulnerability** on this repository.
Don't open a public issue for security problems. You'll get a response within 7 days.

## Design notes

| Threat | Mitigation |
|---|---|
| Data exfiltration | No `INTERNET` permission (enforced with `tools:node="remove"`). No backup/transfer of app data. |
| Fake "payment received" SMS from a person | SMS from personal phone numbers / contact names are ignored. |
| Payer typing text into the remark field to trigger a false announcement | Remark / narration fields are cut off before parsing. |
| Chat or social apps showing "Rs 5000 received" | Email, chat, social and browser apps are blocked by package. |
| Money *requests* and failed transactions read as income | Request, failed, OTP, telecom, top-up and promotion filters. |
| Duplicate / replayed notifications | Reference-number and re-post de-duplication, persisted references for 24 h. |
| Shoulder-surfing the statement | Optional app lock (biometric or screen lock) and `FLAG_SECURE` while it's on. |
| Other apps abusing our components | Only the launcher activity and the listener (protected by `BIND_NOTIFICATION_LISTENER_SERVICE`) are exported. |
| Debug logs leaking notification text | Verbose/debug/info logs are stripped from release builds by R8. |
| Same amount from two customers on two wallets | Treated as two payments when each has its own reference; only wallet + bank SMS pairs are merged. |
| Other Paisa Aayo builds (debug + release installed together) | Their test notifications are ignored. |
| Locked out after removing the phone's screen lock | The app lock turns itself off when the phone has no lock left (removing it already needs the old PIN). |

## Reliability

Tested on an Android 15 emulator with the release (R8-shrunk) build:

| Situation | Result |
|---|---|
| Phone rebooted, app never opened | Listener re-binds and the keep-alive notification starts by itself |
| App process killed (`kill -9`) | Android restarts it; the next payment is announced |
| Text-to-speech engine killed or updated | Engine is restarted and the announcement retried once |
| 45 payments in 15 seconds | All 45 saved immediately; announcements follow in order |
| Corrupt settings file | Reset to defaults instead of stopping the listener |
| App left open past midnight / clock or timezone changed | "Received today" and History roll over |

## Known limitations

- **Fake notifications from installed apps.** Any app on the phone can post a notification that
  *looks* like a wallet's. Install apps only from the Play Store, turn off sources you don't use in
  *Settings → Sources*, and confirm large payments in your wallet or bank app.
- **Android 15+ hides some notifications.** Android hides notifications that look like they
  contain a one-time code from apps like Paisa Aayo. That includes many payments whose transaction
  ID or reference is 6–8 characters long (a 10–12 digit RRN is fine). Only system apps can read
  them. Paisa Aayo can't announce these; instead Home shows "Check eSewa" (or whichever payment
  app it was) so the payment isn't missed silently. Hidden SMS are skipped, because those are
  usually real OTPs.
