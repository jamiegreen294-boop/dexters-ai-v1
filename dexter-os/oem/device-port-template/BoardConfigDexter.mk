# Include after the OEM's real BoardConfig.mk.
# Values are intentionally not guessed: they come from the selected BSP.

# REQUIRED production properties/checks:
# - BOARD_AVB_ENABLE := true
# - Virtual A/B or A/B OTA configuration supplied by OEM
# - release-key AVB chain and rollback indexes
# - SELinux enforcing
# - vendor_boot / init_boot configuration matching the selected Android release
# - eUICC/RIL/IMS vendor integration
# - exact camera/audio/display/sensor HAL set
