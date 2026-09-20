# Dexter Business Phone release signing

Dexter Business Phone uses one permanent Android signing identity for Device Owner updates.

Certificate SHA-256:

`47:21:DB:BA:48:0F:1F:D8:68:EF:A0:11:F8:E6:89:6C:EC:A1:B3:03:77:29:D7:07:F0:55:8E:E2:73:35:05:08`

The private keystore is intentionally **not stored in this repository**.

The manual workflow `Build Dexter Android Release APK` expects these GitHub Actions repository secrets:

- `DEXTER_ANDROID_KEYSTORE_B64`
- `DEXTER_ANDROID_KEYSTORE_PASSWORD`
- `DEXTER_ANDROID_KEY_ALIAS`
- `DEXTER_ANDROID_KEY_PASSWORD`

Future Device Owner updates must use this same signing identity.
