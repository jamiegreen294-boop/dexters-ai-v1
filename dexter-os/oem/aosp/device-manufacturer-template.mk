# Example only. The OEM replaces <vendor>/<device> with its real product.
$(call inherit-product, device/<vendor>/<device>/device.mk)
$(call inherit-product, vendor/dexter/dexter_os_common.mk)

PRODUCT_NAME := dexter_<device>
PRODUCT_DEVICE := <device>
PRODUCT_BRAND := Dexter
PRODUCT_MODEL := Dexter Phone
PRODUCT_MANUFACTURER := <oem>

# Keep telephony/camera/audio packages supplied by the device vendor.
# Dexter OS replaces customer-facing launcher/settings/setup surfaces,
# not the radio or HAL implementation.
