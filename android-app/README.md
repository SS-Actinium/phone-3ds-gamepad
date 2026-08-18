# Android app (Hinge Pad)

Kotlin + Jetpack Compose controller. Landscape only. Sends UDP JSON to the Windows server.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug / Meerkat or newer).
2. **File → Open** and choose this `android-app` folder (not the repo root).
3. Let Gradle sync. If the wrapper JAR is missing, Studio will download Gradle **8.10.2**.
4. Connect a phone (USB debugging) or start an emulator. Use a **real phone on Wi-Fi** for actual play.
5. Run the `app` configuration.

`minSdk` 26 · `compileSdk` / `targetSdk` 35 · Compose BOM `2026.01.00`.

## Where to enter the PC IP

Launch the app → **PC IP address** and **UDP port** (default `26760`) → **Test** or **Connect**.

Last values are stored in app preferences.

## Permissions

Internet only. No location, storage, or accessibility access.
