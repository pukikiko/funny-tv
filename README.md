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
