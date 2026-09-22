# Dexter Phone Hardware Bring-Up Contract

Before Dexter OS can be sold on a handset, the OEM/device partner must provide and validate:

1. Boot chain: unlocked engineering target for bring-up, then production AVB/verified boot.
2. Device tree: BoardConfig, product makefiles, partition map and fstab.
3. Kernel: source/config or compliant prebuilt plus modules.
4. Vendor blobs: graphics, camera, audio, sensors, GNSS, Wi-Fi/Bluetooth, DRM where required.
5. Telephony: modem firmware, RIL, IMS/VoLTE/VoWiFi, emergency-calling certification and carrier profile.
6. Camera/audio: HAL validation for every sensor, microphone and speaker route.
7. Power: suspend/resume, charging, thermal limits and battery health.
8. Security: hardware-backed keystore, SELinux enforcing, verified boot, rollback protection and production keys.
9. Compliance: UKCA/CE, SAR/radio approvals, emergency-call behaviour, privacy and update commitments.
10. Factory provisioning: Dexter OS image flashing, serial/IMEI handling, device-owner enrollment and OTA enrollment.

The current Meizu managed-shell prototype proves the Dexter UX/control model. It is not a substitute for this hardware bring-up.


## eSIM / eUICC
- production GSMA-compliant eUICC fitted to the exact retail SKU
- Android feature android.hardware.telephony.euicc exposed
- working OEM/AOSP LPA backend
- modem eUICC terminal capabilities and default boot profile support
- Radio HAL SIM-power and slot-status support for the Android release
- physical SIM + eSIM DSDS preferred
- Android 13+ MEP support preferred for dual active eSIM profiles
- production SM-DP+ interoperability evidence required
