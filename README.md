# Funny TV

An Android TV frontend for [funny] a self-hosted short-form media platform. Enjoy your shitpost folder from the comfort of your couch!

## Controls

The interface is designed for standard Android TV / Google TV remotes (D-Pad navigation):

| Button | Action |
| :--- | :--- |
| **DOWN** | Skip to next video |
| **UP** | Go back to previous video |
| **RIGHT** | Like (👍) |
| **LEFT** | Dislike (👎) |
| **CENTER (Short Press)** | Play / Pause |
| **CENTER (Hold)** | Open Settings menu (change instance URL) |

## Installation & Build Instructions

1. Clone this repository:
   ```bash
   git clone https://github.com/pukikiko/funny-tv.git
   ```
2. Open the `funny-tv` directory in **Android Studio**.
3. Let Gradle sync and download the required dependencies (Jetpack Compose, Media3/ExoPlayer, Retrofit).
4. Run the application on an Android TV Emulator or connect your physical Android TV device via ADB.
