# Approved Device Actions

The following actions are eligible for autonomous execution after the Meizu agent has been provisioned and paired with Dexter AI:

1. Health/status checks.
2. Read Dexter app version and running state.
3. Launch or restart allow-listed Dexter apps.
4. Gather Dexter-specific diagnostic logs.
5. Check connectivity to approved Dexters services.
6. Install/update a signed Dexter APK only when the package, source and hash match the approved release metadata.
7. Verify the updated app and automatically mark the update failed if health checks fail.
8. Prepare rollback to the previous signed Dexter build.

Owner approval is required for:

- rebooting the handset;
- rollback to a different version;
- changing Android permissions or protected settings when Android presents an approval screen;
- adding a new third-party application to the allow-list;
- any operation outside this document.

bOnline call handling and the POS call integration are explicitly excluded.
