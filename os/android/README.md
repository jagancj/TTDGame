# Building OpenTTD for Android

This directory contains documentation and resources for building OpenTTD for Android.

## Prerequisites

| Component | Minimum Version | Notes |
|-----------|-----------------|-------|
| Android SDK | API 35 (Android 15) | Configured in `android/app/build.gradle.kts` |
| Android NDK | r25c+ (e.g. 26.x / 27.x) | Install via Android Studio SDK Manager |
| CMake | 3.22.1+ | Install via Android Studio SDK Manager |
| JDK | 17+ | Bundled with modern Android Studio |
| SDL3 / SDL2 | SDL 3.x / 2.x | Source checkout in `android/SDL3` or via `SDL3_PATH` |

## Project Structure

```text
android/
├── app/
│   ├── build.gradle.kts             # Modern Kotlin DSL Gradle build script (Target SDK 35)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml      # Manifest with immersive landscape and permissions
│       ├── java/org/openttd/openttd/
│       │   └── MainActivity.kt      # Kotlin activity extending SDLActivity
│       └── res/values/
│           ├── strings.xml
│           └── styles.xml
├── build.gradle.kts                 # Root Gradle build script
├── settings.gradle.kts              # Settings with repository management
├── gradle.properties
├── gradlew / gradlew.bat            # Gradle wrapper scripts
└── gradle/wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties    # Gradle 8.10.2
```

## Build Instructions

### 1. Build Host Tools
Compile `strgen` and `settingsgen` for your host machine:
```bash
cmake -B build-host -S . -DOPTION_TOOLS_ONLY=ON
cmake --build build-host --target tools
```

### 2. Build the Android Project
From the repository root:
```bash
cd android
./gradlew assembleDebug
```
(On Windows command prompt / PowerShell, use `.\gradlew.bat assembleDebug`.)

### 3. OpenGFX Base Graphics
The Gradle build includes an automated task (`copyOpenGFXBaseGraphics`) that:
- Packages built-in baseset files from `media/baseset/`.
- Downloads and unpacks official OpenGFX base graphics into `app/src/main/assets/baseset/` if not already present.
- At runtime, `MainActivity.kt` extracts them into `context.filesDir/baseset/` so OpenTTD finds them immediately upon launch.

