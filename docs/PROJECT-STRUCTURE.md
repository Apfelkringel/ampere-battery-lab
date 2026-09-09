# Project structure

Ampere Battery Lab is an Android-first project. The Android source and release
workflow are kept separate from optional web prototyping and local test state.

```text
android/                    Native Android app, Gradle build and resources
artifacts/                  Locally delivered debug/release APK copies
docs/                       Project notes and structure documentation
prototypes/web/             Optional Vite/React visual prototype
tooling/android-avd/        Local emulator images and state (not source data)
.github/workflows/          CI release build and signing checks
latest.json.example         Example for the public update manifest
README.md                   User/install/development documentation
```

The Android app stores user readings at runtime in the app sandbox. Those
runtime readings are not part of this repository. The public update repository
contains only the signed APK, its hash manifest and release documentation.

Generated directories (`android/build`, `android/app/build`, `android/.gradle`,
`prototypes/web/node_modules`, `prototypes/web/dist` and the local AVD state)
are ignored by Git. They are retained on the external SSD only when the local
workspace needs them for reproducible testing.

The canonical workspace location is:

`/Volumes/MacSSD/02_PROJECTS/Active/AccuBattery`
