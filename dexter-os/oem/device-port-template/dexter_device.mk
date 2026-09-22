$(call inherit-product, device/<vendor>/<device>/device.mk)
$(call inherit-product, vendor/dexter/dexter_os_common.mk)

PRODUCT_NAME := dexter_<device>
PRODUCT_DEVICE := <device>
PRODUCT_BRAND := Dexter
PRODUCT_MODEL := Dexter Phone
PRODUCT_MANUFACTURER := <oem>

PRODUCT_SYSTEM_PROPERTIES += \
    ro.dexter.hardware_sku=<sku> \
    ro.dexter.ota.manifest=<signed_manifest_endpoint>
