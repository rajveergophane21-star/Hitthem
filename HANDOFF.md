# Grudge Booth — Android handoff

Everything needed to build the APK in a fresh chat with GitHub connected.
**Nothing here needs a local JDK or Android SDK** — CI does the build.

---

## 1. What this is

A throwing game. You pick a photo, then throw darts, tomatoes, eggs, paint balloons,
rocks and snowballs at it. Endless, no score. Two views: **Flat** (default — the photo
fills the screen) and **Room** (the photo hangs on a nail on a wall, swings when hit,
and debris piles on the floor).

The whole game is one self-contained HTML file with no build step and no dependencies.
This Android project is a thin WebView wrapper around it.

Live web version: https://claude.ai/artifact/TPdSbArcfFddRMm8fkCYz8

---

## 2. Repo layout

**Make the repo from THIS folder** (`grudge-booth-android`), so it is the repo root.
The CI workflow assumes that.

```
.github/workflows/android.yml     CI: builds the APK on every push to main
app/
  build.gradle                    AGP 8.5.2, minSdk 26, targetSdk 34, Java 17
  src/main/
    AndroidManifest.xml           VIBRATE + INTERNET, single Activity, no rotation reload
    assets/index.html             THE GAME — one file, ~53 KB, runs offline
    java/com/grudgebooth/app/MainActivity.java
    res/values/{strings,colors,themes}.xml
    res/drawable/ic_launcher_foreground.xml    vector icon (tomato + dart)
    res/mipmap-anydpi-v26/ic_launcher.xml      adaptive icon
build.gradle  settings.gradle  gradle.properties
gradle/wrapper/gradle-wrapper.properties       (no wrapper jar — see below)
```

---

## 3. Push it, get an APK

```bash
cd grudge-booth-android
git init -b main
git add .
git commit -m "Grudge Booth Android wrapper"
gh repo create grudge-booth --private --source=. --push
```

The **Build APK** workflow runs automatically. Then either:

- **Actions tab** → latest run → *Artifacts* → `grudge-booth-debug-apk`, or
- `gh run download -n grudge-booth-debug-apk`, or
- push a tag for a download link you can open on the phone:
  `git tag v1.0 && git push --tags` → the APK is attached to a GitHub Release.

Install it: copy to the phone and open it (allow "install unknown apps" for your file
manager), or `adb install -r app-debug.apk`.

It is signed with the standard debug key — fine for your own phone, not for the Play Store.

---

## 4. Things that are easy to get wrong (already handled)

| Thing | Why it matters |
| --- | --- |
| `WebChromeClient.onShowFileChooser` in `MainActivity` | Without it `<input type="file">` **silently does nothing** in a WebView and the "Photo" button is dead. This is the #1 reason wrapped web games break on Android. |
| `VIBRATE` permission | `navigator.vibrate` fails silently without it; the game buzzes on every impact. |
| `android:configChanges="orientation\|screenSize\|..."` | Without it, rotating the phone recreates the Activity, reloads the page and **wipes the mess you have thrown**. |
| `minSdk 26` | Lets the launcher icon be adaptive-XML-only, so no PNG icon assets are needed anywhere. |
| No `gradlew` / wrapper jar | The wrapper jar is a binary and is not committed. CI installs Gradle itself and runs `gradle assembleDebug`. Locally, run `gradle wrapper --gradle-version 8.7` once if you want `./gradlew`. |
| `FLAG_KEEP_SCREEN_ON` + immersive mode | It is a game; the screen should not sleep and the bars should be out of the way. |
| Offline | The only network fetch is the Archivo webfont from Google Fonts. Offline it falls back to the system font; everything else works with no connection. |

---

## 5. Updating the game

The game is **one file**: `app/src/main/assets/index.html`. Edit it directly, or replace
its body with a new export — keep the `<!doctype html>` head block at the top, which
supplies the viewport meta, `color-scheme: dark` and the safe-area padding that the
web host normally injects. Commit and push; CI rebuilds.

Internals, if you need them: a 2D canvas game in one IIFE. `dec` holds the accumulated
mess, `#fx` is the board surface, `#airCv` draws objects in flight in front of it.
Projectiles fly with a real `1/z` perspective projection and cast a shadow onto the board
that converges on the impact point. `RB` is the board's spring/pendulum state.

---

## 6. Wish list / not done

- Release signing with a real keystore (needs your own key + repo secrets).
- Bundling the Archivo font as a local `@font-face` for a fully offline typeface.
- Saving the defaced photo to the gallery (needs a `MediaStore` bridge from JS).
