# Paisa Aayo · पैसा आयो

**A free, open-source payment speaker for Android.** Your phone reads out every payment you
receive on eSewa, Khalti, Fonepay QR, IME Pay, ConnectIPS and bank SMS, in **Nepali or English**.
It's a "soundbox" for shops, with no extra hardware.

> "एक हजार दुई सय पचास रुपैयाँ प्राप्त भयो" · "Received one thousand two hundred fifty rupees"

- **No ads, no accounts, no tracking**
- **No internet permission**: payment data physically cannot leave the phone
- **Nepali and English** UI and announcements, with lakh/crore grouping and Devanagari digits
- **Guided first-run setup**: language, permissions, battery, then a test announcement
- **Robust**: scam/promo/OTP filtering, de-duplication, a watchdog that re-binds the listener
- **Optional app lock** (fingerprint / face / screen lock) and a local payment statement

## Install

Download the latest `PaisaAayo-x.y.z.apk` from
[Releases](https://github.com/mk-manishkandel/Paisa-Aayo/releases) and verify it against
`SHA256SUMS.txt`. Android 8.0+.

On Android 13+, sideloaded apps need one extra tap before notification access can be granted:
**Settings → Apps → Paisa Aayo → ⋮ → Allow restricted settings**. The setup screen links there.

## How it works

```
 eSewa / Khalti / Fonepay / bank SMS
            │  notification
            ▼
 PaymentListenerService  (NotificationListenerService, bound by Android)
   1. ignore email, chat, social and browser apps
   2. SMS: ignore personal numbers / contact names  ← blocks "I sent you Rs 5000" scams
   3. strip remark fields                           ← payer-typed text is never read aloud
   4. drop OTP, money requests, failed txns, telecom, top-up, promotions
   5. classify credit vs debit (English + नेपाली)
   6. extract amount (paisa-exact) and RRN / Txn ID
   7. de-duplicate (reference, re-posts, wallet + SMS for the same payment)
            │
            ├──▶ Room database (amount, direction, source, reference, time; never the text)
            └──▶ Announcer: recorded Nepali voice pack → or text-to-speech (ne → hi → en)
                 with audio focus and optional volume boost

 WatchdogWorker (every 15 min): re-binds the listener if Android dropped it, prunes history
```

| Module | What's in it |
|---|---|
| `parser/` | Pure Kotlin, no Android: detection pipeline, Nepali/English number words, money formatting, de-duplication. Fully unit-tested. |
| `app/` | Jetpack Compose UI, listener service, announcer, Room, DataStore, WorkManager. |
| `tools/voice/` | Recording script for the Nepali voice pack. |

## Build

Requirements: JDK 17 and the Android SDK (platform 35).

```bash
./gradlew test lintDebug assembleDebug   # app/build/outputs/apk/debug/
```

Every push and pull request runs the same command in GitHub Actions (see `.github/workflows/ci.yml`),
and the debug APK is attached to the run.

## Releasing

1. Create a signing key once and keep it safe. If it's lost, users can't update.
   ```bash
   keytool -genkeypair -v -keystore release.jks -alias paisaaayo -keyalg RSA -keysize 4096 -validity 10000
   base64 -i release.jks | pbcopy   # macOS; on Linux: base64 -w0 release.jks
   ```
2. Add repository secrets (**Settings → Secrets and variables → Actions**):
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag and push: `git tag v1.0.0 && git push origin v1.0.0`. The *Release* workflow builds,
   signs and publishes the APK with a checksum file.

## Contributing

Issues and pull requests are welcome, especially:
- **Notification samples** that are missed or misread. Please redact names, numbers and balances,
  then add them as a test in `parser/src/test/.../PaymentParserTest.kt`.
- **A recorded Nepali voice pack** (see [`tools/voice/README.md`](tools/voice/README.md)).

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [PRIVACY.md](PRIVACY.md) and
[docs/DESIGN.md](docs/DESIGN.md).

## License

[GPL-3.0](LICENSE). Forks must stay open source. Fonts: IBM Plex Sans and Noto Sans Devanagari
under the [SIL Open Font License](licenses/).

Paisa Aayo is an independent project and is not affiliated with eSewa, Khalti, Fonepay, NCHL or
any bank.
