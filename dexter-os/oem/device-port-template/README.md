# Dexter OS device port template

Copy this directory into the selected handset's device tree and replace every
<vendor>/<device>/<board> placeholder with the OEM BSP values.

The common Dexter OS software is already separated from the hardware port. The
retail port must provide the kernel/vendor/modem/eUICC/camera/audio/display
implementation and inherit `vendor/dexter/dexter_os_common.mk`.

Do not mark a port retail-ready until RELEASE-GATES.md passes on the physical SKU.
