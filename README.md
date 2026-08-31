# Nursery Slideshow

A web app that turns a batch of photos into a finished slideshow video — built for nursery/kindergarten teachers (or anyone else) who don't have time to edit video by hand.

Just select your photos and the app builds a slideshow for you. A short wizard also lets you adjust things along the way if you want: how photos are grouped onto pages, per-photo crop shapes (rectangle / rounded / circle / oval), a theme, background music, and how long each slide is shown.

## Features

- Upload and reorder photos, then group them into pages (1–3 photos per page)
- Per-photo crop shape: rectangle, rounded corners, circle, or oval
- Multiple visual themes with decorative backgrounds/frames
- Background music selection with in-app preview
- Adjustable per-slide duration, with a real slideshow length of `slide count × seconds`
- Asynchronous video generation with live progress, powered by FFmpeg
- Responsive UI (desktop and mobile)

## Tech Stack

- **Frontend**: Vue 3 (Composition API), TypeScript, Vite, Pinia
- **Backend**: Java 21, Spring Boot 3, Spring Data JPA, Flyway
- **Database**: MySQL 8
- **Video generation**: FFmpeg (invoked as a subprocess; filter_complex-based compositing/crossfades)

## Prerequisites

- Java 21 (a JDK, not just a JRE)
- Node.js (LTS)
- MySQL 8
- FFmpeg (a reasonably recent build with `libx264`), available on your `PATH` or pointed to via an env var (see below)

## Setup

### 1. Database

Create a database and a user for the app (adjust the password as you like):

```sql
CREATE DATABASE nursery_slideshow CHARACTER SET utf8mb4;
CREATE USER 'nursery_app'@'localhost' IDENTIFIED BY 'nursery_app';
GRANT ALL PRIVILEGES ON nursery_slideshow.* TO 'nursery_app'@'localhost';
```

The schema itself is created automatically on startup via Flyway migrations (`backend/src/main/resources/db/migration`) — no manual schema setup needed.

### 2. Backend

```bash
cd backend
./gradlew bootRun
```

Configuration (`backend/src/main/resources/application.yml`) reads from environment variables, all with sensible local-dev defaults:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_USERNAME` | `nursery_app` | MySQL username |
| `DB_PASSWORD` | `nursery_app` | MySQL password |
| `STORAGE_ROOT_DIR` | `./storage` | Where uploaded photos/generated videos are stored |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | Origin allowed to call the API |
| `FFMPEG_BINARY_PATH` | `ffmpeg` | Path to the FFmpeg executable |
| `FFMPEG_TITLE_FONT_PATH` | `C:/Windows/Fonts/meiryo.ttc` | Font used to render the title text overlay — **override this on macOS/Linux** to a font file that supports Japanese (e.g. a Noto Sans CJK path) |
| `FFMPEG_TIMEOUT_SECONDS` | `120` | Max seconds to wait for a single FFmpeg run before it's killed |

The server starts on port `8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts on port `5173` and talks to the backend via `VITE_API_BASE_URL` (see `frontend/.env`, already set to `http://localhost:8080`).

### 4. Background music files

The BGM tracks are **not** included in this repository (see [Licensing](#licensing) below). Download three tracks of your choice — any length is fine, they're looped/trimmed automatically to fit the video — from one of the sites below, and place them at:

```
backend/storage/bgms/bright.mp3
backend/storage/bgms/moving.mp3
backend/storage/bgms/energetic.mp3
```

- [DOVA-SYNDROME](https://dova-s.jp/)
- [甘茶の音楽工房 (Amacha Music)](https://amachamusic.chagasi.com/)

## Licensing

This project's own code has no separate license file yet (all rights reserved by default).

The background music this app was built and tested with came from the two free-music sites linked above. Under both sites' terms:

- Commercial and non-commercial use is allowed.
- DOVA-SYNDROME requires no credit; Amacha Music doesn't require credit either, but appreciates one (site name "甘茶の音楽工房", artist name "甘茶", or the site URL).
- **Redistributing the audio files themselves (e.g. committing them to a public repo) is not allowed by either site.** That's why they're excluded here — see [Setup step 4](#4-background-music-files).
