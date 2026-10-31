# AVATEK

AVATEK is a Persian-first, offline music player for Android with a premium black, metallic-gold, glass and animated visual identity.

## Platform and architecture

- Native Android app written in Kotlin.
- UI: Jetpack Compose + Material 3.
- Playback: AndroidX Media3 ExoPlayer and MediaSessionService.
- Library discovery: Android MediaStore; no online music catalog or account is required.
- Local persistence: SharedPreferences-backed stores for favorites, playlists, listening history, settings, lyrics and playback queue.
- Minimum Android version: Android 8.0 (API 26); target SDK 35; compile SDK 36.
- JDK 17 and Gradle 8.14.3 in CI.

## Features

### Home and discovery
- Local daily mix ranked using favorites, play counts, recency and recently added tracks.
- Discovery mix for less-played and recently added music.
- Echo Radio: a local station seeded from the current track, matching genre, artist and album.
- Smart Crate: dynamic offline collections for favorites, unplayed tracks, recent additions, most-played tracks and rediscovery.
- Listening insights: total play starts, unique tracks heard, favorites, top artists, most-played tracks and recent history.

### Music library
- Songs, albums, artists, folders, genres and playlists.
- Local search by title, artist, album, folder and genre.
- Sort by title, artist, recently added, duration and most played.
- Favorites and editable playlists.
- MediaStore rescanning and configurable minimum track duration.

### Playback
- Background playback through MediaSessionService.
- System media notification and headset/media-button integration.
- Full-screen player with animated vinyl artwork, waveform seek bar and live audio visualizer.
- Queue controls: play next, append, reorder, remove and clear.
- Previous/next, seek, repeat modes, shuffle and playback speed.
- Queue, current track, position, repeat, shuffle and speed are persisted for session restoration.

### Sound Lab
- Device-supported equalizer bands.
- Bass boost and virtualizer where the Android device supports them.
- Saved effect levels and presets.
- Playback speed and sleep timer, including stop-at-end-of-track.

### Lyrics
- Import local LRC files through Android's document picker.
- Timestamp parsing supports multiple timestamps per line and global LRC offset.
- Lyrics are stored locally per track and synchronized to playback position.
- No network lookup is performed.

### Settings and privacy
- Multiple accent themes, glass intensity, animations, ambient artwork colors, visualizer, haptics and notification style.
- Local JSON backup and restore for AVATEK app data, including settings, favorites, playlists, history, queue, EQ preferences and imported lyrics.
- Backup does not copy the user's music files; those remain in their original folders.
- Listening data and imported lyrics stay on the device unless the user explicitly exports a backup.

## Build and validation

GitHub Actions runs:

    gradle --no-daemon --continue --console=plain :app:testDebugUnitTest :app:assembleDebug

Unit tests cover LRC timestamp parsing, multiple timestamps, offsets and metadata filtering. Smart Crate collections are calculated on demand from the local library and listening history. Review the latest Actions run on foundation/ui-redesign for the authoritative build and APK artifact status. Real-device checks are still required for notifications, Bluetooth/headset controls, audio focus, MediaStore differences and backup/restore.

## Development policy

- Keep the playback engine and existing features stable; avoid unrelated rewrites.
- UI inspiration may come from Flutter/Dart and native Android music-player projects, but AVATEK remains a native Kotlin/Compose application.
- Do not copy third-party source code without reviewing its license and attribution requirements.
- Develop on foundation/ui-redesign; keep main untouched until the complete build and device-level verification are approved.
