# AVATEK

AVATEK is an offline-first Android music player with a premium black, metallic-gold, and blood-red visual identity.

## Implemented foundation

- Kotlin + Jetpack Compose + Material 3
- Android application module using Gradle Kotlin DSL
- Minimum Android version: Android 8.0 (API 26)
- Media3 ExoPlayer and MediaSessionService playback engine
- Local audio discovery through MediaStore with runtime permission handling
- Search across track title, artist, and album
- Persistent local favorites and a favorites-only filter
- Track list metadata including artist, album, and duration
- Play, pause, previous, next, and track selection
- Persian-first dark luxury interface
- GitHub Actions workflow to compile the debug APK

## Build prerequisites

- Android Studio with Android SDK 36 installed
- JDK 17
- Gradle 8.14.3 or compatible version

The project currently does not include Gradle Wrapper files. The CI workflow installs Gradle directly. A successful compile has not yet been confirmed, so the pull request remains a draft until GitHub Actions reports a successful build.

## Next milestones

1. Confirm the first green CI build and fix any compile/runtime issues.
2. Add a full now-playing screen with seek bar, elapsed/remaining time, and repeat/shuffle.
3. Add dedicated album and artist browsing screens.
4. Add editable playlists and queue management.
5. Add lyrics, visualizer, and equalizer integration after playback stability is verified.

AVATEK is designed for local playback first. No account, cloud library, or online music catalog is assumed.
