# Dexter OS generic arm64 product foundation.
# A real handset product should inherit this file from its device-specific product.

$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

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

PRODUCT_PACKAGES += \
    DexterLauncher \
    DexterSettings \
    DexterSetup \
    DexterUpdater

# Resource overlays replace customer-facing Android labels/branding.
PRODUCT_PACKAGE_OVERLAYS += vendor/dexter/overlay

# Production handset products must add their own telephony/camera/audio packages
# and vendor HALs. This generic product deliberately does not fake hardware support.
