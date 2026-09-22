# Dexter OS OTA

Retail Dexter Phones use signed device-specific OTA payloads.

The common OS defines the policy and manifest contract. The chosen OEM/device
port must provide:

- A/B or Virtual A/B partition/update-engine integration;
- release and AVB signing keys held outside source control;
- signed payload metadata;
- rollback index management;
- failed-update recovery;
- a stable-channel release manifest matching release-manifest.schema.json.

No generic OTA image is shipped from this repository because an OTA payload is
inseparable from the selected device's partition map, boot chain, kernel/vendor
images and signing keys.
