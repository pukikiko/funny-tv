# Funny TV

An Android frontend for [funny] a self-hosted short-form media platform. Enjoy your shitpost folder from the comfort of your couch, or from the toilet.

Two variants are built from one codebase:

| Variant | Build | Launcher |
| :--- | :--- | :--- |
| **tv** | `assembleTvRelease` | Leanback (Android TV / Google TV) |
| **mobile** | `assembleMobileRelease` | Standard (phones and tablets) |

## Features

Everything the web player does:

- Feed with the server's **PipeAI Algorithm** or **Randomised** mode
- Watch history, so you don't get the same clips twice
- 👍 / 👎 voting with live counts
- Per-video **copy link**, **share** and **download**
- **Upload** to the instance's moderation queue
- Persistent volume, mute, and seeking

## Controls

### TV (D-Pad)

| Button | Action |
| :--- | :--- |
| **DOWN** | Skip to next video |
| **UP** | Go back to previous video |
| **RIGHT** | Like (👍) |
| **LEFT** | Dislike (👎) |
| **CENTER (Short Press)** | Play / Pause |
| **CENTER (Hold)** or **MENU** | Open Settings menu |
| **FAST FORWARD / REWIND** | Seek 10 seconds |

The Settings menu holds the instance URL, feed mode and volume. Sharing, downloading
and uploading are mobile-only — there is nowhere useful to put them on a TV.

### Mobile (touch)

| Gesture | Action |
| :--- | :--- |
| **Swipe up** | Skip to next video |
| **Swipe down** | Go back to previous video |
| **Tap** | Play / Pause |
| **Drag the bottom bar** | Seek |

The feed tracks your finger and settles on release, and the next video is always
buffering on a second player so a skip starts instantly.

The action rail sits bottom right, like the web player: volume, feed mode, voting,
share, upload and settings. Settings is its own screen.

## Install with Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) installs the APK straight from
this repo's GitHub releases and keeps it updated. Tap the button for your device —
it opens Obtainium with the source, the APK filter and the app name already filled in.

| Android TV / Google TV | Phones and tablets |
| :---: | :---: |
| <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%7B%22id%22%3A%22com.pukikiko.funny%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpukikiko%2Ffunny-tv%22%2C%22author%22%3A%22pukikiko%22%2C%22name%22%3A%22Funny%20TV%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22funny-tv-%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Afalse%7D%22%7D"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Add Funny TV to Obtainium" height="70"></a> | <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%7B%22id%22%3A%22com.pukikiko.funny%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpukikiko%2Ffunny-tv%22%2C%22author%22%3A%22pukikiko%22%2C%22name%22%3A%22Funny%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22funny-mobile-%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Afalse%7D%22%7D"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Add Funny to Obtainium" height="70"></a> |

The buttons go through `apps.obtainium.imranr.dev`, which bounces to the
`obtainium://` link. GitHub strips custom URL schemes out of README links, so it
cannot be linked directly; the redirect page is part of the Obtainium project and
carries the config in the URL rather than storing anything. If your browser refuses
the hand-off, turn on "Allow forward URL requests to external intents" (or similar)
in its settings.

To add it by hand instead — on a TV, where there is no convenient browser:

1. **Add App** → URL: `https://github.com/pukikiko/funny-tv`
2. Set **Filter APKs by Regular Expression** to the variant you want:
   - `funny-tv-` for Android TV / Google TV
   - `funny-mobile-` for phones and tablets
3. Leave the rest at the defaults and hit **Add**.

The filter matters: every release carries both variants, and without it Obtainium
asks which APK to take on each update. Both variants share the applicationId
`com.pukikiko.funny`, so only one can be installed on a given device at a time.

## Installation & Build Instructions

1. Clone this repository:
   ```bash
   git clone https://github.com/pukikiko/funny-tv.git
   ```
2. Open the `funny-tv` directory in **Android Studio**.
3. Let Gradle sync and download the required dependencies (Jetpack Compose, Media3/ExoPlayer, Retrofit).
4. Pick the variant in the **Build Variants** panel — `mobileDebug` for a phone,
   `tvDebug` for a TV — then run it on an emulator or a device over ADB.

Or from the command line:

```bash
./gradlew assembleMobileDebug   # or assembleTvDebug
```

CI builds both variants on every push and signs them with the committed test key
(`app/testkey.jks`), so the APKs from a workflow run install straight onto a device.
That key is public — it is only ever for test builds.

## Releasing

Releases are what Obtainium sees, so they come from a tag:

```bash
git tag v1.2.3 && git push origin v1.2.3
```

`.github/workflows/release.yml` then builds both flavors, checks each APK's
signature and version, and publishes `funny-tv-1.2.3.apk` and
`funny-mobile-1.2.3.apk` to a GitHub release named `v1.2.3`. Running the workflow
manually with a version instead creates the tag for you, and a tag such as
`v1.2.3-rc.1` is published as a pre-release (which Obtainium ignores unless
"Include prereleases" is on).

Versions must be `X.Y.Z`. The tag drives `versionName` and a `versionCode` of
`major * 1000000 + minor * 1000 + patch`, so tags have to keep climbing — Android
refuses to install an APK whose `versionCode` is lower than the installed one.
Local and PR builds have no tag and report version `0.0.0`.

Because the signing key never changes, an Obtainium update installs over the
previous build. Swapping in a real key would break updates for everyone who
installed the test-key build — they would have to uninstall first.
