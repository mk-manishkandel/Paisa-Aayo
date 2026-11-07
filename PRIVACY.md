# Privacy policy: Paisa Aayo

**Effective date:** 7 November 2026
**App:** Paisa Aayo: Payment Speaker (`io.github.mkmanishkandel.paisaaayo`)
**Developer:** Manish Kandel ([hello@manishkandel.com.np](mailto:hello@manishkandel.com.np))

Paisa Aayo announces payments you receive by reading payment notifications on your phone.
It is built so your money data **cannot** leave your phone.

## Summary

- We do **not** collect, transmit, sell or share any personal or financial data.
- The app has **no internet permission**, so Android itself blocks all network access, including
  from libraries. You can check this under *Settings → Apps → Paisa Aayo → Permissions*.
- No analytics, no crash reporting, no ads, no accounts, no device identifiers.

## What the app reads

With your permission (*Notification access*), the app reads the text of new notifications **in
memory only**, to decide whether each one is a payment. Notifications from email, chat,
social-media and browser apps are discarded without being parsed.

## What the app stores, and where

For each detected payment it stores the **amount, whether it was received or paid, the source
app's name, the reference number and the time**. The full notification text is never stored.
When Android hides a payment app's notification content (Android 15 and later), only that app's
name and the time are kept, to show you a reminder.

If *Read bank SMS* is on, the app also remembers the **sender IDs of business SMS** it has seen
(for example `NABIL_ALERT` or `31001`), up to 50, so you can turn individual senders off in
*Settings → Sources*. Phone numbers and contact names are never stored, and no SMS text is kept.

This data is kept in the app's private storage on your phone. Android cloud backup and
device-to-device transfer are disabled for it, so it is not copied to Google or to other devices.
Nobody, including the developer, can see it.

## How long it is kept, and how to delete it

Payments are kept for 30, 90 (default) or 365 days, as you choose in *Settings*. Older records are
deleted automatically. *Settings → Clear history* deletes all payments and remembered SMS senders immediately, and uninstalling
the app deletes all of its data.

## Permissions

| Permission | Why |
|---|---|
| Notification access | To detect payment notifications and announce them |
| Show notifications | For the optional "Listening for payments" notification and test payments |
| Foreground service | Keeps the listener running so announcements aren't delayed |
| Ignore battery optimisations (optional) | Stops the phone's battery saver from stopping the listener |
| Biometric (optional) | Lets you lock the app with your fingerprint, face or screen lock |
| Run at startup, prevent sleep, view network state | Added by Android's WorkManager library, which runs a daily check that the listener is alive and deletes old history. "View network state" only lets it see whether a connection exists; without the internet permission the app still cannot send or receive anything |

## Children

The app is a tool for shop owners and is not directed at children under 13.

## Changes to this policy

Any change will be published at this address with a new effective date. The full history of this
file is public in the project's repository.

## Contact

Questions or concerns: email [hello@manishkandel.com.np](mailto:hello@manishkandel.com.np), or open
an issue at [github.com/mk-manishkandel/Paisa-Aayo/issues](https://github.com/mk-manishkandel/Paisa-Aayo/issues).
The app is open source, so anyone can verify these statements in its code.
