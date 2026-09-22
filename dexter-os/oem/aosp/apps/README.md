# Dexter customer-facing system apps

These modules are the OEM replacements for the Android/Google surfaces that still appear on the Meizu managed-shell prototype.

- DexterDialer owns ACTION_DIAL plus InCallService UI.
- DexterMessaging is the intended default SMS handler.
- DexterCamera will own image/video capture on the selected OEM hardware.
- DexterFiles owns customer-visible file/download browsing.

The Dialer, Messaging and Files foundations are present here. DexterCamera is hardware-dependent and must be completed against the chosen handset camera HAL/device tree rather than pretending a generic camera implementation is production-ready.

For retail builds, the OEM image must assign the appropriate default roles during provisioning and pass all release gates in ../../RELEASE-GATES.md.
