# Dexter Meizu Device Agent (TEST)

This module is the phone-side management layer for Dexter AI.

## Scope

The agent is for the dedicated Meizu business phone only.

It does **not** manage bOnline calls or the POS call workflow.

### Dexter-managed actions

- report device/app health
- report Android version, battery, storage and network state
- report Dexter app versions
- collect Dexter app crash/diagnostic logs
- launch/restart approved Dexter apps
- download and install approved Dexter APK updates
- roll back to the last known-good Dexter APK
- verify an app after install/update
- report missing permissions and settings that require owner action
- run approved troubleshooting playbooks
- send operation results back to Dexter AI

### Safety

- TEST environment only until owner approval.
- No factory reset, account removal, credential access or arbitrary file deletion.
- No bOnline/POS call-control changes.
- APK installs must be from an approved allow-list and must pass signature/hash verification.
- Risky Android settings changes require owner approval.
- Every command is logged with device ID, request ID, action, result and timestamp.
- Every software update keeps a rollback target.

## Control flow

Dexter AI Command Centre
-> Supabase ai-command-centre
-> device command queue
-> Dexter Device Agent on Meizu
-> action + verification
-> result/diagnostics returned to Dexter AI

## Command contract

See `protocol-v1.json`.

## Provisioning target

Preferred provisioning is Android managed-device / device-owner where the Meizu Android build supports it. The fallback is a normal signed app plus explicit Android permissions and owner-approved install prompts.

Root access is not required by the design and should not be enabled by default.
