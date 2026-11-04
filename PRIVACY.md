# Privacy

Paisa Aayo is built so your money data **cannot** leave your phone.

- **No internet permission.** The app's manifest removes `android.permission.INTERNET`, so Android
  itself blocks any network access, including from libraries. You can check this under
  *Settings → Apps → Paisa Aayo → Permissions*.
- **What it reads:** the text of notifications, in memory only, to decide whether they are
  payments. Email, chat, social-media and browser notifications are discarded without being parsed.
- **What it stores:** for each detected payment, the amount, credit/debit, source app name, reference
  number and time. The notification text is never stored. When Android hides a payment app's
  notification (see SECURITY.md), only that app's name and the time are kept, to show a reminder.
- **Where:** in the app's private storage on your phone. Android backup and device-to-device
  transfer are disabled for this data.
- **How long:** 30, 90 (default) or 365 days, your choice. *Settings → Clear history* deletes
  everything immediately. Uninstalling the app deletes everything too.
- **No analytics, no crash reporting, no ads, no accounts, no device identifiers.**
