# Dexter OS eSIM hardware requirements

eSIM is mandatory for the Dexter retail-phone reference design.

## Required
- GSMA-compliant production eUICC embedded at manufacture.
- Android reports `android.hardware.telephony.euicc`.
- OEM provides a working LPA backend compatible with Android's eUICC framework.
- Modem keeps the eSIM powered with the default boot profile and advertises eUICC capabilities.
- Radio HAL implements the eSIM requirements for the Android release used by the product, including SIM power and slot-status support.
- Production profile download from a real SM-DP+ must pass before release.
- UK/EU radio bands, VoLTE, emergency calling and carrier certification must be validated on the exact SKU.

## Preferred v1 layout
- 5G modem.
- One nano-SIM slot plus one soldered eUICC.
- Dual-SIM dual-standby.
- Multiple Enabled Profiles (MEP) support where the SoC/eUICC combination supports it.
- NFC, Wi-Fi 6 or better, Bluetooth 5.x, USB-C.
- Hardware-backed keystore, AVB and locked bootloader.

## Dexter UI
The customer uses Dexter eSIM to:
- add a profile by QR/activation code;
- see installed eSIM profiles;
- enable/switch profiles;
- remove profiles when policy allows;
- see carrier/profile status.

The retail UX must not redirect the customer to stock Android eSIM settings.

## Carrier/network separation
Dexter OS eSIM support does not itself provide mobile service. Connectivity comes from a carrier/MVNO/eSIM provider that supplies a GSMA-compatible profile through SM-DP+. The hardware/OS and the mobile-plan commercial agreement are separate workstreams.
