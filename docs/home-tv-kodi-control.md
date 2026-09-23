# Dexter AI — Home TV / Kodi Control

Purpose: allow the Dexter AI test command centre to manage the user's home Fire TV and Kodi setup from phone or web chat through the existing Home PC connector.

## Current device path

- Fire TV ADB endpoint: 192.168.0.236:5555
- Fire TV model: Amazon AFTSSS / sheldonp
- Android ABI: armeabi-v7a
- Kodi package: org.xbmc.kodi
- Local Kodi manager: C:\DexterAI\KodiManager\Dexter-Kodi.ps1
- ADB executable: C:\DexterAI\tools\platform-tools\adb.exe

## Supported actions

Natural-language requests can map to:
- open Kodi -> Dexter-Kodi.ps1 -Action launch
- close Kodi -> Dexter-Kodi.ps1 -Action stop
- check Kodi -> Dexter-Kodi.ps1 -Action status
- back up Kodi -> Dexter-Kodi.ps1 -Action backup
- restore Kodi -> Dexter-Kodi.ps1 -Action restore with an explicit backup path
- install/update an approved APK -> Dexter-Kodi.ps1 -Action install-apk with an explicit local APK path

Always verify ADB is connected before action. If the Fire TV is unauthorized, ask the owner to approve the prompt shown on the TV.

## Boundary

This is home entertainment control only. Do not modify the live Dexters POS, Loyalty App, KDS, Back Office, menu, payments, customer data, or other business systems.

## Phone workflow

Use the Dexter AI test web app as the phone interface. The owner can open the deployment in a mobile browser and add it to the home screen. The command centre should translate natural-language TV requests into Home PC actions using this file as the operational reference.
