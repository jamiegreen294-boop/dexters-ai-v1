# Dexter Phone Reference Hardware v1

This is the procurement target for the first retail Dexter OS handset. A supplier may propose an equivalent or better reference design, but no item marked REQUIRED may be removed.

## REQUIRED
- Android 15+ device tree and vendor support suitable for an AOSP-derived product.
- 64-bit ARM SoC with production kernel/vendor sources or a contractual BSP/update path.
- 5G sub-6 modem supporting UK networks and VoLTE/VoWiFi integration.
- GSMA-compliant production eUICC soldered into the handset.
- Android `android.hardware.telephony.euicc` support and a working LPA backend.
- One nano-SIM slot plus eSIM for the first SKU.
- Dual-SIM dual-standby between nano-SIM and eSIM.
- Camera HAL/source integration for all fitted lenses.
- Audio HAL, proximity sensor, earpiece, loudspeaker and Bluetooth-call routing.
- Wi-Fi, Bluetooth, GNSS and USB-C.
- Hardware-backed Android Keystore.
- AVB 2.0, rollback protection and production-locked bootloader.
- OEM-controlled boot animation, setup, launcher, Settings/SystemUI overlays and OTA.
- UKCA/CE/RoHS documentation for the exact hardware SKU and radio configuration.
- OTA/security-support commitment suitable for a product sold in the UK.

## PREFERRED
- Multiple Enabled Profiles (MEP) capable eUICC/modem.
- NFC.
- Wi-Fi 6.
- Bluetooth 5.2+.
- 6 GB RAM / 128 GB storage or better.
- 4500 mAh+ battery.
- 1080p-class display.
- Fingerprint sensor.
- A/B seamless OTA partitions.

## DEXTER SOFTWARE HANDOFF
The OEM must integrate:
- DexterOS privileged system app
- DexterDialer + Dexter In-Call UI
- DexterMessaging
- DexterFiles
- DexterEsim LUI
- Dexter Camera implementation against the selected camera HAL
- Dexter Setup/Home/Settings/SystemUI overlays
- Dexter OTA and factory provisioning policies

## eSIM ACCEPTANCE
The handset is rejected if any of these fail:
- EID can be read.
- Production SM-DP+ profile can be downloaded.
- Profile survives reboot and OTA.
- Profile can be switched/disabled/deleted under policy.
- Emergency calling works on eSIM.
- VoLTE works on the intended UK carrier.
- nano-SIM + eSIM DSDS works if advertised.
- Dexter eSIM UI is used instead of stock Android eSIM Settings.

## SUPPLIER DELIVERABLES
- exact SoC/modem/eUICC part numbers
- Android BSP version and security patch baseline
- device tree / BoardConfig / vendor blob delivery terms
- modem RIL/IMS package
- LPA/eUICC integration details
- camera/audio/display/touch HALs
- AVB/signing/OTA integration process
- certification pack and SAR evidence
- MOQ, engineering/NRE cost, per-unit price at 50/100/250/500/1000 units
- sample/prototype lead time
- production lead time
- software/security support duration
