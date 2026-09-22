#!/bin/bash
set -euo pipefail

ADB="${ADB:-adb}"

fail(){ echo "FAIL: $*" >&2; exit 1; }
pass(){ echo "PASS: $*"; }

$ADB wait-for-device

$ADB shell getprop ro.dexter.os | grep -q true || fail "Dexter OS property"
pass "Dexter OS identity"

$ADB shell cmd role get-role-holders android.app.role.HOME | grep -q uk.co.dextersspot.dexterai || fail "Dexter Home role"
$ADB shell cmd role get-role-holders android.app.role.DIALER | grep -q uk.co.dexter.os.dialer || fail "Dexter Dialer role"
$ADB shell cmd role get-role-holders android.app.role.SMS | grep -q uk.co.dexter.os.messaging || fail "Dexter SMS role"
pass "Core roles"

$ADB shell pm list features | grep -q android.hardware.telephony.euicc || fail "eUICC feature"
pass "eSIM hardware feature"

$ADB shell getenforce | grep -q Enforcing || fail "SELinux not enforcing"
pass "SELinux"

$ADB shell getprop ro.boot.verifiedbootstate | grep -q green || fail "Verified boot is not green"
pass "Verified boot"

$ADB shell settings get global adb_enabled | grep -q '^0$' || fail "ADB enabled on retail device"
pass "ADB disabled"

echo "Core factory gates passed. Continue with radio/camera/audio/eSIM/emergency-call manual certification suite."
