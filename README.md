# Grudge Booth — Android

A one-Activity WebView wrapper around the game. The whole game is bundled in
`app/src/main/assets/index.html`, so it runs offline and no photo ever leaves the phone.

## What you need

This machine has **no JDK, no Android SDK and no Gradle**, so the `.apk` could not be built
here — only the project source. To produce the APK you need either:

- **Android Studio** (easiest — it ships its own JDK and SDK), or
- a **JDK 17** + the Android **command-line tools** + `platforms;android-34` and `build-tools;34.0.0`.

## Build it with Android Studio

1. Open Android Studio → **Open** → pick this `grudge-booth-android` folder.
2. Let it sync (it downloads Gradle and the SDK bits, and writes the Gradle wrapper jar).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
5. Copy it to the phone and install it (allow "install unknown apps" for your file manager),
   or with the phone plugged in and USB debugging on: `adb install -r app-build...apk`.

## Build it from the terminal

With JDK 17 and the SDK installed, and `ANDROID_HOME` pointing at the SDK:

```
cd grudge-booth-android
gradle wrapper --gradle-version 8.7     # once, to create ./gradlew
./gradlew assembleDebug
```

A debug APK is signed with the local debug key, which is fine for installing on your own
phone. For a Play-Store build use `./gradlew assembleRelease` and sign it with your own
keystore — see https://developer.android.com/studio/publish/app-signing

## What the wrapper does

| Concern | Handling |
| --- | --- |
| Picking a photo | `WebChromeClient.onShowFileChooser` opens the system picker and hands the image back to the page's file input. Without this, `<input type="file">` silently does nothing in a WebView. |
| Vibration | `VIBRATE` permission, so `navigator.vibrate` fires on each impact. |
| Fullscreen | Immersive mode; system bars come back on a swipe. |
| Rotation | Handled via `configChanges`, so turning the phone does not reload the game. |
| Screen sleep | `FLAG_KEEP_SCREEN_ON`. |
| Back button | Finishes the Activity. |
| Internet | Only used to fetch the Archivo webfont. Offline it falls back to the system font and everything else still works. |

## Updating the game

Re-export the page and overwrite `app/src/main/assets/index.html`, keeping its
`<!doctype html>` head block. Then rebuild.
