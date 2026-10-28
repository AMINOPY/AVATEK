# AVATEK

AVATEK is an offline-first Android music player concept with a premium black, metallic-gold, and blood-red visual identity.

## Current foundation

- Kotlin + Jetpack Compose + Material 3
- Android application module and Gradle Kotlin DSL configuration
- Minimum Android version: Android 8.0 (API 26)
- Media3 ExoPlayer and MediaSessionService foundation for background playback
- Audio-library permission declarations
- Initial Persian home screen and shared dark luxury theme

## Build prerequisites

- Android Studio with Android SDK 36 installed
- JDK 17
- Gradle sync from Android Studio

Open this repository in Android Studio and sync Gradle. The Gradle wrapper is not included yet; add it from a local Gradle installation or Android Studio before relying on command-line CI builds.

## Roadmap

1. Implement local audio discovery through MediaStore with runtime permission handling.
2. Connect the Compose UI to MediaController and the MediaSessionService.
3. Add queue, playback controls, album/artist views, and persistent playlists.
4. Add lyrics, visualizer, and equalizer integration after the playback foundation is validated.

AVATEK is designed for local playback first. No account, cloud library, or online music catalog is assumed.
