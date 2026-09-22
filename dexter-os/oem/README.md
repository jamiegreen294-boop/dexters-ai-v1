# Dexter OS OEM Foundation

This folder is the manufacturer/AOSP handoff layer for Dexter OS.

## Goal
Ship a phone where the customer uses Dexter-owned surfaces for setup, home, settings, control centre, updates and approved apps, instead of exposing stock Android Settings/Launcher during normal operation.

## What is implemented here
- AOSP product definition for an arm64 Dexter OS build target
- Dexter product properties and branding
- privileged-app allowlist
- default-permission policy
- SystemUI and Settings resource-overlay hooks
- setup/provisioning defaults
- OTA/update-channel policy
- OEM hardware bring-up contract

## What still requires an OEM/device partner
A retail build cannot be produced safely from the APK alone. The manufacturer must supply:
- device tree / BoardConfig
- proprietary vendor blobs
- kernel and boot configuration
- radio/IMS/RIL integration
- camera HAL
- audio HAL
- display/touch HAL
- verified-boot keys and AVB configuration
- production signing keys
- OTA server signing and release process

Dexter OS v43 remains the working managed-shell prototype on the Meizu. This OEM layer is intentionally separate so it cannot break the current phone.
