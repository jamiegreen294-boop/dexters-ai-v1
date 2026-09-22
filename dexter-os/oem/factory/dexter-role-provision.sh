#!/system/bin/sh
set -eu

# Factory-only role assignment. Run after the Dexter packages are installed.
cmd role add-role-holder android.app.role.HOME uk.co.dextersspot.dexterai 0
cmd role add-role-holder android.app.role.DIALER uk.co.dexter.os.dialer 0
cmd role add-role-holder android.app.role.SMS uk.co.dexter.os.messaging 0

# Confirm the holders so the factory line fails closed.
cmd role get-role-holders android.app.role.HOME 0 | grep -q uk.co.dextersspot.dexterai
cmd role get-role-holders android.app.role.DIALER 0 | grep -q uk.co.dexter.os.dialer
cmd role get-role-holders android.app.role.SMS 0 | grep -q uk.co.dexter.os.messaging
