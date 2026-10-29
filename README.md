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
- Persistent offline playlist storage with create, add, remove, and delete operations
- Main overflow menu with library, favorites, playlists, rescan, settings/about entry points
- Playlist manager dialog with create, list, track counts, and delete
- Track metadata: artist, album, and duration
- Track selection, play/pause, previous/next, and playback seek bar
- Persian-first dark luxury interface
- GitHub Actions workflow for debug APK compilation

## Build prerequisites

- Android Studio with Android SDK 36 installed
- JDK 17
- Gradle 8.14.3 or compatible version

The repository does not currently include Gradle Wrapper files. CI installs Gradle directly. The Android SDK setup has been fixed and the first Gradle compilation is being checked; review the linked Pull Request's Checks tab for the current result.

## Next milestones

1. Confirm a green CI build and address any compile or runtime issues.
2. Add dedicated album and artist browsing screens.
3. Add editable playlists, queue controls, repeat, and shuffle.
4. Add lyrics, visualizer, and equalizer integration after playback stability is verified.

AVATEK is designed for local playback first. No account, cloud library, or online music catalog is assumed.
