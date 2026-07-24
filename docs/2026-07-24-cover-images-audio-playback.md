# Cover Images & Audio Playback Fix

**Date:** 2026-07-24

## Problem
- Podcast cover images were not showing in the list or detail screens
- Audio files were not playing because `MediaPlayer` needs full URLs but only storage paths were being used
- The `priv` bucket is private, so `publicUrl()` doesn't work for audio playback

## Solution

### 1. Cover Images with Coil
- Added **Coil 2.7.0** dependency for image loading
- `PodcastAdapter` loads cover images into `ivThumbnail` using Coil with `CircleCropTransformation`
- `DetailFragment` loads cover into new `ivCover` ImageView with `RoundedCornersTransformation`
- Falls back to violet circle with first letter when no cover URL exists

### 2. Audio Playback with Signed URLs
- Added `createSignedUrl()` method to `SupabaseService` using `kotlin.time.Duration`
- Added `extractStoragePath()` to parse bucket + path from stored URLs
- Added `resolveAudioUrl()` that generates signed URLs for private buckets
- `DetailFragment.initMediaPlayer()` now calls `resolveAudioUrl()` before `setDataSource()`

### 3. Detail Layout Update
- Added `ivCover` ImageView inside the cover `MaterialCardView`
- When cover loads successfully, `ivCover` becomes visible and `txtCoverLetter` hides

## Files Changed
- `gradle/libs.versions.toml` — Coil 2.7.0
- `app/build.gradle.kts` — Coil dependency
- `SupabaseService.kt` — signed URL methods, URL extraction
- `PodcastAdapter.kt` — Coil image loading
- `DetailFragment.kt` — cover image + signed audio URL
- `fragment_detail.xml` — ivCover ImageView
