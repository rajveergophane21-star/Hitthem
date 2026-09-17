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
| Sizing the mess | A small `GrudgeHost` JS bridge runs `android.media.FaceDetector` over a downscaled copy of the picked photo and hands back the subject's face width, so a splat is sized against the face rather than against the frame. Entirely on-device; the picture still never leaves the phone. The bridge is why the `WebViewClient` refuses to navigate anywhere but `file:///android_asset/`. |
| Fullscreen | Immersive mode; system bars come back on a swipe. |
| Rotation | Locked to portrait - the layout is built for a phone held upright. `configChanges` stays so any other config change (dark mode, keyboard) does not reload the page and wipe the mess. |
| Screen sleep | `FLAG_KEEP_SCREEN_ON`. |
| Back button | Finishes the Activity. |
| Internet | Only used to fetch the Archivo webfont. Offline it falls back to the system font and everything else still works. |

## Screen mode - throwing over other apps

**Screen** in the toolbar asks for "display over other apps", then starts
`OverlayService`, which puts two windows on top of everything else: a transparent
WebView running `assets/overlay.html`, and a draggable tomato bubble.

The big window is `FLAG_NOT_TOUCHABLE` by default, so the app underneath scrolls
normally and the splats just sit in front of it. Tapping the bubble makes it
touchable so taps become throws, and it hands scrolling back on its own 1.5s after
the last throw. Long-press the bubble to wipe, drag it to move it.

Two limits are structural, not bugs:

- **Splats stick to the screen, not to what is under them.** Nothing lets one app
  read another's scroll position or content, so when the feed moves the splats do
  not. Tracking content would mean continuous screen capture and optical flow.
- **You cannot throw and scroll at once.** A window either takes a touch or lets it
  through, and no API hands a touch to another app, so it has to be a mode.

`assets/splats.js` is the splat art, extracted verbatim from `index.html` so both
the game and the overlay draw the same mess from one source.

## Updating the game

Re-export the page and overwrite `app/src/main/assets/index.html`, keeping its
`<!doctype html>` head block. Then rebuild.
