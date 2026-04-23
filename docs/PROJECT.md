# NextVideo — project overview

## Goal

NextVideo is an Android app that lets users browse and play **videos stored on Nextcloud**.

Core requirements:

- **Multi-account** (more than one Nextcloud server/user).
- **Library + search** (video list with filtering).
- **Streaming playback**.
- **Offline downloads** (user-controlled, app-private storage).

## Non-goals (for the MVP)

- No “video wallpaper” features.
- No server-side indexing: the app relies on WebDAV listing and local caching.

## Architecture (high level)

- **UI (Compose)**: screens for accounts, library, player, downloads.
- **Auth**: Nextcloud **Login Flow v2** → returns `loginName` + `appPassword`.
- **Networking**:
  - WebDAV `PROPFIND` for listing.
  - WebDAV `GET` for streaming and downloads.
  - Auth via `Authorization: Basic …` built from loginName/appPassword.
- **Storage**:
  - Account secrets (app password) in **EncryptedSharedPreferences**.
  - Metadata (accounts, video entries, download state, playback position) in **Room**.
- **Offline**:
  - Downloads done with **WorkManager** and persisted status/progress in Room.

## Security & privacy principles

- Never log tokens / `Authorization` headers.
- Prefer HTTPS; no cleartext by default.
- Offline media stored in app-private directories unless explicit export is added later.

## Roadmap (suggested)

- MVP: login → list videos → search → play streaming → download offline.
- Next: resume playback position, “Continue watching”, favorites.
- Next: subtitles (sidecar `.srt/.vtt` and embedded tracks where possible).
- Next: better thumbnails (Nextcloud preview endpoints) and sorting.

