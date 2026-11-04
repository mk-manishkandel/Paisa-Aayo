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

**Known limitation:** any installed app can post a notification that *looks* like a wallet's. If
you need certainty, turn off sources you don't use in *Settings → Sources*, and always confirm
large payments in your wallet or bank app.
