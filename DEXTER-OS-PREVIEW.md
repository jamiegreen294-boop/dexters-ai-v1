# Dexter OS Preview

This branch is an isolated, non-destructive test bed for a future Dexter OS experience.

## Goal
Build and test a complete Dexter-branded phone environment on the existing Meizu Android installation before any bootloader unlock, factory reset, ROM flashing, or vendor partition changes are attempted.

## Phase 1 — OS Preview on stock Android
- Dexter boot handoff screen immediately after Android boot
- Dexter Home as default launcher
- Dexter Control Centre
- Dexter Settings
- Staff / Owner profiles
- Managed Internet
- Business apps
- Notifications surface
- Phone / contacts / messages shortcuts
- Camera / files / calculator
- Device health
- Remote support and self-update
- Locked admin controls for staff
- Recovery path via the existing PC agent

## Phase 2 — Hardware feasibility
Before any wipe, verify:
- bootloader unlock availability
- fastboot/fastbootd access
- Project Treble / GSI compatibility
- dynamic partitions
- AVB / vbmeta state
- vendor blobs and kernel requirements
- modem, Wi-Fi, Bluetooth, camera, audio, sensors, NFC and charging support

## Phase 3 — Real Dexter OS candidate
Only if Phase 2 proves the Meizu can be safely recovered:
- AOSP-based build or compatible GSI base
- Dexter SystemUI / Launcher / Settings
- signed OTA updates
- recovery image and rollback plan
- full hardware validation

## Safety rule
No wipe, bootloader unlock, factory reset, vbmeta change, partition flash or ROM install is allowed from this branch without explicit owner approval and a verified recovery image/rollback path.
