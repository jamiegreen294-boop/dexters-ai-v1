# Common Dexter OS product integration.
PRODUCT_PACKAGES += \
    DexterOS \
    DexterDialer \
    DexterMessaging \
    DexterFiles

PRODUCT_COPY_FILES += \
    vendor/dexter/permissions/privapp-permissions-dexter.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-dexter.xml \
    vendor/dexter/permissions/default-permissions-dexter.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/default-permissions/default-permissions-dexter.xml

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.dexter.os=true \
    ro.dexter.edition=commercial \
    ro.dexter.channel=stable \
    ro.setupwizard.mode=DISABLED

PRODUCT_PRODUCT_PROPERTIES += \
    persist.sys.locale=en-GB

# Device-specific products should inherit this file after their vendor/device product.
