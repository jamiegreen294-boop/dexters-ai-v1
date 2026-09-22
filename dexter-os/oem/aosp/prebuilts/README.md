# DexterOS.apk input

Place the release Dexter OS APK at `prebuilts/DexterOS.apk` during an OEM/AOSP build.

For a production image, AOSP must sign it with the product platform certificate. Do not use the GitHub test keystore as the retail platform key.

The package name remains:
`uk.co.dextersspot.dexterai`

The OEM build includes it under `/product/priv-app` through the `DexterOS` module.
