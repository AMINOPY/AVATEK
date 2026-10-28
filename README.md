# AVATEK

AVATEK is an offline-first Android music player with a premium black, metallic-gold, and blood-red visual identity.

## Foundation implemented

- Kotlin + Jetpack Compose + Material 3
- Android application module using Gradle Kotlin DSL
- Minimum Android version: Android 8.0 (API 26)
- Media3 ExoPlayer and MediaSessionService for the playback engine
- Local audio discovery through MediaStore
- Runtime permission handling for local audio access
- Track list, tap-to-play, current-track label, and basic play/pause controls
- Initial Persian home screen and shared dark luxury theme
- GitHub Actions workflow to compile the debug APK

## Build prerequisites

- Android Studio with Android SDK 36 installed
- JDK 17
- Gradle 8.14.3 or compatible version

The repository currently does not include the Gradle Wrapper files. Android Studio may sync the project, and the included GitHub Actions workflow installs Gradle directly for CI. A successful compile has not yet been confirmed; check the Actions run associated with the pull request.

## Next milestones

1. Verify the first CI build and resolve any compilation issues.
2. Add robust playback state handling, queue controls, previous/next, and seek support.
3. Improve library navigation with albums, artists, folders, and search.
4. Add persistent playlists and favorites.
5. Add lyrics, visualizer, and equalizer integration after playback stability is verified.

AVATEK is designed for local playback first. No account, cloud library, or online music catalog is assumed.
