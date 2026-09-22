# Dexter OS generic arm64 product foundation.
# This target is for common-product validation only.
# A retail handset must inherit dexter_os_common.mk from its device-specific product.

$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)
$(call inherit-product, vendor/dexter/dexter_os_common.mk)

PRODUCT_NAME := dexter_os_arm64
PRODUCT_DEVICE := dexter
PRODUCT_BRAND := Dexter
PRODUCT_MODEL := Dexter Phone
PRODUCT_MANUFACTURER := Dexter

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.dexter.os=true \
    ro.dexter.channel=stable \
    ro.dexter.edition=commercial \
    ro.product.locale=en-GB

PRODUCT_PACKAGE_OVERLAYS += vendor/dexter/overlay

# This generic target intentionally omits device HALs/vendor images.
# It must never be labelled as a retail flash image.
