# Dexter OS — Product Architecture

Dexter OS is a dedicated mobile operating system for the Dexter team. The first target is the existing Meizu test device, with a path toward a true AOSP-based build after hardware feasibility is proven.

## Product principles
- Dexter-branded from boot handoff through lock screen, home, settings and updates.
- Built for managed business phones, not general consumer customisation.
- Staff-safe by default, owner/admin controls protected.
- Remote support, auditability and recovery are first-class features.
- No dependency on a home PC for normal day-to-day use.
- OTA updates must be signed, staged, health-checked and reversible where possible.

## OS layers

### 1. Base operating system
Long-term target: AOSP-compatible Android base.
- Linux kernel and vendor hardware layer
- Android framework
- System services
- package manager
- networking
- telephony
- storage
- permissions
- encryption
- verified boot where supported

The current Android-based Dexter shell is the test harness, not the final OS base.

### 2. Dexter System UI
Own visual system replacing the ordinary Android presentation where supported:
- boot handoff / startup brand
- lock screen
- status bar
- quick settings / Control Centre
- notifications shade
- volume UI
- power menu
- app switcher
- system dialogs
- Dexter colours, typography and iconography

### 3. Dexter Home
- business-first home screen
- app grid and dock
- swipe pages
- staff shortcuts
- managed Internet
- live order / loyalty / POS / back office shortcuts
- device health indicator
- owner/admin entry

### 4. Dexter Settings
Two levels:
- Staff Settings: Wi-Fi, Bluetooth, sound, display, accessibility, approved account settings
- Owner Settings: policies, debugging, app control, network policy, update channels, diagnostics, recovery

### 5. Dexter Identity
Team sign-in and role system:
- Owner
- Manager
- Staff
- Maintenance / Support
- temporary device session

Role controls determine apps, settings, data access and admin capability.

### 6. Dexter Fleet
Cloud-side device management:
- enrolment
- inventory
- health and online state
- remote app install/update
- remote lock
- lost mode
- reboot
- configuration snapshots
- policy rollout
- audit history
- staged OTA updates
- recovery jobs

### 7. Dexter Browser
Managed browser for staff:
- normal web access
- managed start page
- business bookmarks
- configurable category / site controls
- session controls
- safe remote support hooks
- owner-only policy changes

### 8. Dexter Apps
Core suite:
- Dexter AI
- Loyalty
- POS
- Scanner
- Back Office
- WhatsApp Business
- Gmail
- Maps
- Camera
- Files
- Calculator
- browser
- optional Square / delivery platforms / bOnline where required

### 9. Dexter AI system agent
On-device privileged management service:
- device health
- app inventory
- app launch
- package deployment
- self-update
- policy status
- local recovery bridge
- approved UI automation
- diagnostics
- logs
- update verification

### 10. Update system
Preview stage:
- signed APK updates

Full OS stage:
- signed OTA packages
- update channels: test / beta / stable
- staged rollout
- pre-update checks
- post-update heartbeat
- failed-update rollback where hardware permits

## Team-phone behaviour
A Dexter team phone should boot into Dexter branding, land on Dexter Home, expose only suitable staff controls, keep Dexter AI online in the background and remain remotely recoverable by an authorised owner.

## Hardware gate before real ROM
No real OS flash until the test device passes:
- bootloader unlock verification
- fastboot / fastbootd
- Treble / GSI compatibility
- partition map and dynamic partition checks
- AVB / vbmeta assessment
- full stock firmware backup or known-good recovery package
- modem / IMS
- Wi-Fi
- Bluetooth
- camera
- microphone / speaker
- sensors
- charging / USB
- storage encryption
- suspend / wake
- thermal and battery behaviour

## Safety
Factory reset, bootloader unlock, partition flashing, vbmeta changes, recovery replacement and destructive wipes require explicit owner approval and a tested recovery path.
