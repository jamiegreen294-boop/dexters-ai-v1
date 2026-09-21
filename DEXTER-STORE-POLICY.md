# Dexter Store Policy

Dexter Store is the managed app catalogue for Dexter OS team phones.

## Staff
- Can see approved apps.
- Can open installed approved apps.
- Can install approved apps from Google Play.
- Cannot silently add arbitrary apps to the Dexter catalogue.
- Can submit an app request for owner review.

## Manager
- Same approved catalogue as staff.
- Can recommend apps for teams or roles.
- Cannot approve OS-wide catalogue changes unless explicitly granted owner permission.

## Owner
- Controls the approved catalogue.
- Can add or remove approved apps.
- Can assign apps by role/device group.
- Can approve or reject requests.
- Can stage mandatory apps for future fleet rollout.

## Store source
During the Android/AOSP preview, Dexter Store uses Google Play as the package source for third-party apps. Dexter-owned apps may be delivered through Dexter's signed update channel.

## Full Dexter OS target
For a future AOSP Dexter OS build, Google Play availability will depend on Google Mobile Services licensing/certification for the device build. The store architecture therefore keeps the catalogue separate from the package source so an alternative approved source can be added later without redesigning Dexter Store.
