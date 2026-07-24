# Audify — Agent Guidelines

## Project overview
- **App:** "Audify" — podcast player for Android
- **Language:** Kotlin, minSdk 24, targetSdk 36, compileSdk 36
- **Build:** Gradle Kotlin DSL, AGP 9.1.1, version catalog `gradle/libs.versions.toml`
- **No DI, no ViewModels, no Jetpack Compose** — XML layouts + ViewBinding, logic in Activities/Fragments, `SupabaseService` singleton for data
- **All UI strings are in Spanish** (`res/values/strings.xml`)

## Entrypoints & navigation
- `LoginActivity` is the **launcher** (MAIN/LAUNCHER in manifest)
- `LoginActivity` → `MainActivity` (on login or skip)
- `LoginActivity` → `RegisterActivity` (link in login screen)
- `MainActivity` hosts `NavHostFragment` with bottom nav (4 tabs: Inicio, Mis podcasts, Subir podcast, Mi perfil) + drawer (Favoritos, Listas, Borradores, Cerrar sesión)
- Navigation uses `Navigation.findNavController(view).navigate(R.id.destId)` — **no explicit actions** in nav graph
- **Nav graph destinations:** `inicioFragment`, `podcastsFragment`, `uploadFragment` (argument `draftId: String?`), `profileFragment`, `favoritesFragment`, `listsFragment`, `draftsFragment`, `detailFragment` (argument `podcastId: Int`), `userProfileFragment` (argument `userId: String`)

## Permissions (manifest)
`INTERNET`, `RECORD_AUDIO`, `READ_EXTERNAL_STORAGE` (maxSdk 32), `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`

## Build & run
```powershell
.\gradlew.bat :app:assembleDebug
```
No real tests exist (only auto-generated stubs). No lint/typecheck configured.

## Architecture

### Audio playback & sensors
- **`AudioForegroundService`** — `Service` foreground type `mediaPlayback`, owns `MediaPlayer`. Started via `startForegroundService()` + `bindService()` with `LocalBinder`.
- **Sensors live in the service** (not the fragment): `startSensors()` called from `prepareAndPlay()`, `stopSensors()` from `stop()`.
- **Proximity sensor** (`ProximitySensorManager`): `TYPE_PROXIMITY`. On near → `MODE_IN_COMMUNICATION` + `isSpeakerphoneOn=false` + `PROXIMITY_SCREEN_OFF_WAKE_LOCK`. On far → `MODE_NORMAL` + speaker + release wake lock.
- **Shake detector** (`ShakeDetector`): `TYPE_GYROSCOPE` with fallback to `TYPE_ACCELEROMETER`. Thresholds: gyro 12 rad/s, accel 25 m/s². Requires 2 shakes within 800ms. Fires `togglePlayPause()`.
- **Notification**: `MediaStyle` with actions: rewind 10s, play/pause, forward 10s, stop. Uses `media-compat`.
- **Service lifecycle**: `START_NOT_STICKY`, stops on `ACTION_STOP`.

### Supabase integration
- **Library:** supabase-kt 2.6.1, Ktor 2.3.12, OkHttp engine. Serializer: Jackson.
- **Client:** `SupabaseClient` singleton with `Auth`, `Postgrest`, `Storage` modules.
- **Auth:** `gotrue-kt`. After `signUpWith(Email)`, get current user via `client.auth.currentUserOrNull()`. `loginUser()` calls `getProfile()` to ensure profile row exists.
- **Storage buckets:** `priv` (uploaded audio, private), `pod` (approved podcast audio, public), `covers` (images, public). `resolveAudioUrl()` tries to copy from `priv` to `pod` via `moveAudioFromPrivToPod()`, falls back to original URL.
- **Signatures:** Audio for approved podcasts uses `createSignedUrl()` with 60 min expiry.
- **Profile INSERT** requires RLS policy: `create policy "..." on profiles for insert with check (auth.uid() = id)`
- **Favorites table** requires: `CREATE TABLE IF NOT EXISTS favorites (id UUID DEFAULT gen_random_uuid() PRIMARY KEY, user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE, podcast_id TEXT NOT NULL, created_at TIMESTAMPTZ DEFAULT now(), UNIQUE(user_id, podcast_id))` + RLS policies for INSERT/DELETE/SELECT on `authenticated` role with `auth.uid() = user_id`.

### Data layer
- **`SupabaseService.kt`** — singleton, hardcoded Supabase URL + anon key (public). All DB calls use `withContext(Dispatchers.IO)`. Provides: auth, profile CRUD, podcasts query/insert, favorites CRUD, playlists CRUD, categories, file upload/download, signed URLs.
- **`SessionManager.kt`** — SharedPreferences wrapper for user ID and email.
- **`DraftsManager.kt`** — JSON-based local storage for upload drafts (SharedPreferences). Used by `UploadFragment` to save/restore draft state.
- **`MockData.kt`** — hardcoded fallback sample data.

### Models
- **`model/Podcast.kt`**: `Podcast(id: Int, supabaseId: String, title, author, description, duration, category, audioUrl, coverUrl, userId, approved)`
- **`model/Playlist.kt`**: `Playlist(id: Int, supabaseId: String, name, podcastCount: Int, podcastIds: MutableList<String>)`
- **SupabaseService internal**: `PodcastSupabase` (String id, Jackson-annotated for `postgrest["podcasts"]` decoding).
- Podcast ID mapping: `podcastId.hashCode()` to convert String → Int for local model. `getPodcastByIntId(podcastId: Int)` uses this.

### Upload flow (UploadFragment, ~689 lines)
- Supports: image selection (Coil preview), audio file selection, audio recording (with device microphone), category picker, draft save/load
- Drafts auto-saved to `DraftsManager`, loaded on back navigation via `draftId` argument
- Resolves `content://` URIs to bytes via `SupabaseService.readUriToBytes()`
- Uploads cover to `covers` bucket, audio to `priv` bucket
- Audio recording uses `MediaRecorder`, permission `RECORD_AUDIO`

## Key conventions
- **ViewBinding** enabled — use `XxxBinding.inflate()` / `ActivityXxxBinding.inflate()`
- **lifecycleScope.launch** for coroutines in Activities/Fragments
- **No explicit nav actions** — nav graph only defines destinations; navigate by ID: `findNavController(view).navigate(R.id.destId)`
- Drawer header views: `binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.txtDrawerXxx)`
- Password visibility toggle uses `PasswordTransformationMethod.getInstance()`
- All drawable backgrounds are custom XML shapes under `res/drawable/bg_*`
- **Coil** for image loading (cover images, profile avatars)
- **SwipeRefreshLayout** on podcast lists (InicioFragment, PodcastsFragment)
- **SDP/SSP** for responsive sizing (`libs.sdp.android`, `libs.ssp.android`)
- `network_security_config.xml` allows cleartext for `10.0.2.2` (dev) and Supabase

## Docs
Every change must be documented in `docs/` with a date-prefixed filename: `YYYY-MM-DD-descriptive-name.md`.

## AGENTS.md auto‑update
Before every `git push`, update `AGENTS.md` to reflect the current state of the project (new files, changed architecture, added dependencies, etc.) so the context stays fresh for future agents.

## Palette (violet/purple)
- Violet primary: `#532680`, Violet dark: `#3D1A5E`, Violet light: `#E8DDF7`
- Purple: `#2D0A4A`, Purple light: `#F3E5F5`
- Gradient bg: `#1E1B4B` → `#2D114A`
