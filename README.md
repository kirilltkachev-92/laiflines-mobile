# NodeGraph — Compose Multiplatform (Android + iOS)

A tiny Compose Multiplatform app. A center node is connected by edges to a **random** number (1–5) of outer nodes arranged evenly around it. Tap **Randomize** to regenerate.

## Project layout

```
composeApp/
  src/commonMain/kotlin/com/example/nodegraph/App.kt   <- shared UI (the graph)
  src/androidMain/kotlin/...MainActivity.kt            <- Android entry point
  src/androidMain/AndroidManifest.xml
  src/iosMain/kotlin/...MainViewController.kt          <- iOS entry point
iosApp/iosApp/                                         <- Swift/Xcode shell
```

## One-time setup

The Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) is not checked in here. Generate it once:

```bash
# from the project root (requires a local Gradle >= 8.11 on PATH)
gradle wrapper --gradle-version 8.11.1
```

Alternatively, copy `gradle/wrapper/gradle-wrapper.jar` from any existing Gradle project.

## Running

- **Android**: open the project in Android Studio (Giraffe+), let it sync, then run the `composeApp` configuration on a device/emulator.
- **iOS**: on a Mac, open `iosApp/iosApp.xcodeproj` in Xcode (you may need to generate it via KMP tooling) and Run. The Kotlin framework is built by Gradle automatically via the `composeApp` tasks (`linkDebugFrameworkIosSimulatorArm64`, etc.).

> Note: the iOS Xcode project file (`project.pbxproj`) is intentionally not generated here. Easiest path: use the [Kotlin Multiplatform Wizard](https://kmp.jetbrains.com/) to generate an equivalent skeleton and drop the files under `composeApp/src/commonMain/kotlin/com/example/nodegraph/` and `iosApp/iosApp/` into it.

## What to tweak

- `MAX_OUTER_NODES` in `App.kt` — cap on outer nodes (default 5).
- `randomConfig()` — how randomness is generated (count, rotation).
- `NodeGraph` — radii, colors, stroke width.
