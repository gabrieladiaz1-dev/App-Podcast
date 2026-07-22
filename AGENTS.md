# Audify — Agent Guidelines

## Project overview
- **App:** "Audify" — podcast player for Android
- **Language:** Kotlin, minSdk 24, targetSdk 36, compileSdk 36
- **Build:** Gradle Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- **No DI, no ViewModels, no Jetpack Compose** — XML layouts + ViewBinding, logic in Activities/Fragments, `SupabaseService` singleton for data
- **All UI strings are in Spanish**

## Entrypoints & navigation
- `LoginActivity` is the **launcher** (MAIN/LAUNCHER in manifest)
- `LoginActivity` → `MainActivity` (on login or skip)
- `LoginActivity` → `RegisterActivity` (link in login screen)
- `MainActivity` hosts a single `NavHostFragment` with bottom nav (4 tabs: Inicio, Mis podcasts, Subir podcast, Mi perfil) + drawer (Favoritos, Listas, Cerrar sesión)
- `DetailFragment` accepts `podcastId` (integer argument) — navigated from any podcast list

## Build & run
```powershell
.\gradlew.bat :app:assembleDebug
```
No lint, typecheck, or test tasks configured beyond default Android Studio placeholders.

## Architecture notes
- **`SupabaseService.kt`** — singleton object, hardcoded Supabase URL + anon key (public, safe for client). Provides: auth (register/login/signout), profile CRUD, podcast queries, favorites, file upload, categories. All DB calls use `withContext(Dispatchers.IO)`.
- **`MockData.kt`** — singleton with hardcoded sample data (podcasts, playlists, favorites). Used as fallback when Supabase data isn't wired up.
- **models/**: `Podcast` (Int id), `Playlist` (Int id, mutableList of podcastIds) — in `model/` package
- There is also a separate `Podcast` data class inside `SupabaseService.kt` (String id, used for Supabase `postgrest["podcasts"]` decoding)
- **No tests exist** beyond auto-generated stubs

## Supabase integration
- **Version:** supabase-kt 2.6.1, Ktor 2.3.12, OkHttp engine
- `gotrue-kt` for auth, `postgrest-kt` for DB, `storage-kt` for file uploads
- `signUpWith(Email)` — get current user via `client.auth.currentUserOrNull()` after signup (not from return value)
- `getProfile()` — returns `Profile` directly (never fails), auto-creates missing profiles using email prefix as default name
- Profile INSERT requires RLS policy: `create policy "..." on profiles for insert with check (auth.uid() = id)`
- `loginUser()` calls `getProfile()` after sign-in to ensure every user has a profile row

## Key conventions
- **ViewBinding** enabled — always use `ActivityXxxBinding.inflate()` / `FragmentXxxBinding.inflate()`
- **lifecycleScope.launch** for coroutines in Activities/Fragments
- No explicit navigation actions in nav graph — use `Navigation.findNavController(view).navigate(R.id.destId)` or `setupWithNavController` for bottom nav
- Drawer header views accessed via `binding.navigationView.getHeaderView(0).findViewById<TextView>(R.id.txtDrawerXxx)`
- Password visibility toggle uses `PasswordTransformationMethod.getInstance()`
- All drawable backgrounds use custom XML shapes under `res/drawable/bg_*`
- Supabase credentials are hardcoded in `SupabaseService.kt` (lines 22-23)

## Documentation convention
Every change made to this project must be documented in `docs/` with a date-prefixed filename: `YYYY-MM-DD-descriptive-name.md`.

## Palette (violet/purple)
- Violet primary: `#532680`, Violet dark: `#3D1A5E`, Violet light: `#E8DDF7`
- Purple: `#2D0A4A`, Purple light: `#F3E5F5`
- Gradient bg: `#1E1B4B` → `#2D114A`
