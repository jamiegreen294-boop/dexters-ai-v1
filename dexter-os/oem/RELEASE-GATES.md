# Dexter OS retail release gates

A build cannot be labelled retail-ready until every gate passes.

## OS identity
- Dexter boot animation from cold boot
- Dexter Setup on first boot
- Dexter Home after setup
- Dexter Settings for customer-visible settings
- no selectable stock launcher
- no customer route into stock Android Settings
- Dexter OTA/update screen

## Phone
- incoming/outgoing voice calls
- emergency calling
- VoLTE / VoWiFi where carrier requires it
- proximity sensor and audio-route switching
- Bluetooth calling
- call waiting/hold/conference
- voicemail integration
- dual-SIM behaviour if advertised

## Messaging
- SMS/MMS send and receive
- OTP handling
- emergency/cell broadcast behaviour retained
- RCS only if certified/provider-supported

## Hardware
- camera all lenses/modes
- torch
- microphones/speakers
- Wi-Fi/Bluetooth
- GNSS
- NFC if fitted
- USB
- charging/thermal/battery
- sensors
- fingerprint/biometric if fitted

## Security
- SELinux enforcing
- production AVB keys
- locked bootloader
- hardware-backed keystore
- monthly/defined security update policy
- rollback protection
- factory reset protection/recovery process
- no debug/test secrets in system image

## Compliance
- UKCA/CE and radio/SAR evidence for the exact hardware SKU
- emergency calling validation
- privacy policy and telemetry disclosure
- OSS notices
- update support period defined

## Acceptance
- 72-hour soak test
- clean factory reset and reprovision
- OTA from previous stable release
- failed OTA recovery
- offline boot
- no stock Android customer-facing escape observed in scripted UI test
