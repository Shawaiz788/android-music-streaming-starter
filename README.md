<div align="center">

# 🎵 Amplify — Android Music Player Starter Kit

### A full-featured, Firebase-powered music streaming app for Android — built as a launchpad for your own music product.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-35-blue)
![Firebase](https://img.shields.io/badge/backend-Firebase-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

---

## 📖 Overview

**Amplify** is a production-style, native Android music streaming app (Java) that comes with almost everything a modern music app needs already wired up: authentication, a real-time cloud song catalog, playlists, favorites, offline downloads, an equalizer, lyrics, theming, and even **Shazam-style audio recognition**.

It's not a toy demo — it's a real app skeleton with real architecture (Fragments + Navigation Component, a singleton player engine, a local SQLite cache, and a full Firebase data layer) so you can strip out the parts you don't need and start shipping your own music, podcast, or audio app on top of it.

> 💡 **Why start from this repo instead of from scratch?** Auth flows, database sync, download management, and a working player engine typically take days-to-weeks to get right. This starter gets you past that and straight into building your app's unique features and branding.

---

## ✨ Features

### 🔐 Authentication & Onboarding
- Email/password sign-up and sign-in (Firebase Authentication)
- Email verification flow (`VerifyEmailFragment`)
- Animated onboarding/login view pager
- Splash screen with session check → routes to login or home automatically

### 🏠 Home & Discovery
- Home feed with shimmer loading skeletons (polished loading UX, not blank spinners)
- New Releases carousel
- Top Music charts (driven by a live **play-count** system in the database)
- Explore/recommendation adapters
- Album details pages with full tracklists

### 🔎 Search
- Fast, typo-tolerant search powered by **Algolia**
- Recent searches history, persisted per user

### 🎧 Player Engine
- Custom `PlayerManager` singleton (built on `MediaPlayer`) — single source of truth for playback state across the whole app
- Queue management, shuffle, and repeat
- Full-screen "Now Playing" player sheet
- Built-in **equalizer** dialog
- **Synced lyrics** dialog
- Playback continues correctly across navigation/fragment changes

### 🎙️ Audio Recognition ("Shazam Mode")
- Record a few seconds of audio and identify the playing song using the **AudD** recognition API
- Animated listening UI with real-time amplitude feedback

### 📁 Library Management
- Create, rename, and manage custom **playlists**
- Favorite **songs**, **albums**, and **artists** — each synced live to the user's Firebase profile
- **Offline downloads**: songs and cover art are downloaded and cached locally (SQLite + local file storage), so downloaded tracks play without a network connection
- Dedicated fragments for Favorites, Favorite Tracks, Favorite Albums, Favorite Playlists, Downloads, and Playlist Details

### 👤 Profile & Personalization
- Editable profile (name, profile picture) synced to Firebase
- Profile photo upload pipeline via Google Drive (Apps Script endpoint) — no paid storage bucket required
- **6 color themes** (teal, orange, purple, blue, red, black) with persistence via `SharedPreferences`
- Dark-mode–aware theme resources

---

## 🖼️ App Architecture at a Glance

```
Splash  →  Auth Check
              │
     ┌────────┴────────┐
     ▼                  ▼
 SignIn/SignUp     MainActivity (Bottom Nav + Nav Graph)
                         │
        ┌────────────────┼─────────────────────┐
        ▼                ▼                     ▼
     Home / Search   Favorites / Downloads   Profile
        │                │                     │
        └──────── PlayerManager (singleton) ────┘
                         │
                Firebase Realtime Database
              (songs, users, playlists, favorites)
```

- **UI layer**: Fragments + `ViewPager2` + Navigation Component + Bottom Navigation
- **Data layer**: One dedicated Firebase "Handler" class per feature (`FirebaseSongsHandler`, `FirebaseUserHandler`, `FirebasePlaylistHandler`, `FirebaseFavouriteSongsHandler`, `FirebaseFavouriteAlbumsHandler`, `FirebaseFavouriteArtistHandler`, `FirebaseRecentSearchHandler`) — a clean pattern to extend if you add new data types
- **Local cache**: `DBManager` + `SQLiteOpenHelper` for downloaded songs, so playback never depends on the network once a track is downloaded
- **Playback**: `PlayerManager` singleton wraps `MediaPlayer` and exposes a listener interface so any screen can observe play state

---

## 🔥 Firebase — What It's Used For & Why

This starter treats Firebase as its entire backend, which means **you don't need to write or host a custom server** to launch a working music app. Here's exactly how each piece is used:

| Firebase Service | Used For | Where |
|---|---|---|
| **Firebase Authentication** | Email/password sign-up, sign-in, session persistence, email verification | `SignInSignup`, `SigninFragment`, `SignUpFragment`, `VerifyEmailFragment` |
| **Firebase Realtime Database** | Song catalog, new releases, play counts, user profiles, playlists, and all favorites (songs/albums/artists) | Every `Firebase*Handler.java` class |
| **google-services.json** | Auto-configures the app to talk to your Firebase project (already included, pointed at a demo project) | `app/google-services.json` |

**Why Realtime Database instead of Firestore?** It keeps the data model simple (plain JSON tree) and gives you live listeners (`ValueEventListener`) out of the box — perfect for a catalog that needs to update instantly across devices (e.g., new play counts, new releases) without writing extra sync logic.

**Data shape (Realtime Database), roughly:**
```
musicplayer-xxxx-default-rtdb/
├── songs/                      → full song catalog (title, artist, url, cover, lyrics...)
├── play_counts/                → per-song play counts (drives "Top Music")
└── users/
    └── {userId}/
        ├── info/                → profile: name, photo, etc.
        ├── playlists/           → user-created playlists
        ├── favouriteSongs/
        ├── favouriteAlbums/
        ├── favouriteArtists/
        └── recentSearches/
```

### ⚠️ Before you ship: make this your own Firebase project
The repo ships with a working demo `google-services.json` and a hardcoded database URL (`musicplayer-xxxxx-default-rtdb...`) so the app can run immediately. **Before releasing your own app, you must:**
1. Create your own project in the [Firebase Console](https://console.firebase.google.com/)
2. Enable **Authentication** (Email/Password provider) and **Realtime Database**
3. Download your own `google-services.json` and replace `app/google-services.json`
4. Update the database URL string in each `Firebase*Handler.java` file to point at your new instance
5. Set proper Realtime Database security rules before going to production (the demo rules are permissive for development only)

---

## 🧰 Full Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Min / Target / Compile SDK | 24 / 35 / 35 |
| Backend / Auth / DB | Firebase Authentication + Firebase Realtime Database |
| Search | Algolia Search API |
| Song recognition | AudD Music Recognition API |
| Image loading | Glide |
| Loading states | Facebook Shimmer |
| Local persistence | SQLite (`SQLiteOpenHelper`) |
| Networking (custom calls) | OkHttp |
| Profile photo hosting | Google Apps Script + Google Drive (no paid bucket needed) |
| Navigation | Android Navigation Component + `ViewPager2` + Bottom Navigation |
| Build system | Gradle (Kotlin DSL), AGP 9.1.1 |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 11
- An Android device or emulator running **API 24+**
- A free [Firebase](https://firebase.google.com/) account
- (Optional, for full functionality) API keys for **Algolia** and **AudD**

### 1. Clone & open
```bash
git clone <your-fork-url>
cd music_player-main
```
Open the folder in Android Studio and let Gradle sync.

### 2. Connect your Firebase project
- Replace `app/google-services.json` with the one from **your** Firebase project
- Enable **Email/Password** sign-in under Authentication
- Create a **Realtime Database** instance and update the database URL used across the `Firebase*Handler.java` classes

### 3. Add your API keys
- **Algolia**: add your Application ID + Search API key where the search client is initialized
- **AudD**: replace `AUDD_API_TOKEN` in `MusicRecognitionFragment.java` with your own token from [audd.io](https://audd.io/)

### 4. Seed your song catalog
Push a few sample entries into the `songs` node of your Realtime Database (title, artist, cover URL, audio URL, lyrics, genre, duration) — the home feed, search, and player all read from this node.

### 5. Run it
Hit ▶️ Run in Android Studio, or:
```bash
./gradlew installDebug
```

---

## 🧑‍💻 Customization Ideas — Make It Your Own App

This is meant to be a **starting point**, not a finished product. Some natural next steps:

- 🎨 Rebrand: swap the app name (`Amplify`), icon, and color themes in `ThemeHelper.java` / `colors.xml`
- ☁️ Swap in your own audio hosting/CDN instead of raw file URLs
- 💳 Add a subscription/paywall layer (there's already a themed "subscription card" drawable to build on)
- 📊 Extend `play_counts` into a full analytics/recommendation pipeline
- 🔔 Add push notifications for new releases via Firebase Cloud Messaging
- 🌐 Migrate the Realtime Database to Firestore if you need more complex querying at scale
- 📱 Port the UI to Jetpack Compose while keeping the same Firebase data layer

---

## 📂 Project Structure (key files)

```
app/src/main/java/com/example/musicplayer/
├── MainActivity.java              → Hosts bottom navigation + nav graph
├── Splash.java                    → Entry point, session check
├── SignInSignup / SigninFragment / SignUpFragment / VerifyEmailFragment
├── HomeFragment / SearchFragment / TopMusicFragment / ReleasesFragment
├── AlbumDetailsFragment / PlaylistDetailsFragment
├── FavoritesFragment / FavouriteTracksFragment / FavouriteAlbumsFragment / FavouritePlaylistFragment
├── DownloadsFragment / DownloadUtils.java / DBManager.java
├── MusicRecognitionFragment.java  → AudD-based song recognition
├── PlayerManager.java             → Core playback engine (singleton)
├── ProfileFragment.java / DriveUploader.java
├── ThemeHelper.java                → Theme persistence & switching
└── Firebase*Handler.java           → One data-access class per Firebase feature
```

---

## 📄 License

This starter project is provided as-is for learning and as a foundation for your own apps. Add your preferred license (MIT recommended) before distributing.

---

<div align="center">

**Built to be forked, rebranded, and shipped. Happy building! 🚀**

</div>
