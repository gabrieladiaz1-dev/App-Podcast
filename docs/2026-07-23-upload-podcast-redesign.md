# 2026-07-23 — Upload Podcast Section Redesign + Borradores

## What changed

### UploadFragment.kt (complete rewrite)
- **Image picker**: Opens gallery via `ActivityResultContracts.GetContent("image/*")`, shows centerCrop preview in 160dp rounded card
- **Audio picker**: Opens file explorer for audio files (MP3/WAV/M4A), with storage permission handling for Android 13+
- **Audio recording**: Uses `MediaRecorder` (API 31+ compatible), saves to app-private external storage as `.m4a`
- **Audio preview player**: Full preview before upload — play/pause button, SeekBar with real-time progress, current/total time display
- **Draft mode**: Loads draft data from `DraftsManager` when `draftId` argument is passed
- **Cover upload**: Reads URI bytes, uploads to Supabase Storage `pod` bucket under `covers/{userId}/`
- **Audio upload**: Uploads to Supabase Storage `pod` bucket under `audios/{userId}/`
- **Podcast insert**: Calls `insertPodcast()` with `approved=false` for admin review
- **Validation**: Title required, description required, audio required — all with inline error messages
- **Success dialog**: "Podcast enviado" with check icon, message about admin review, button to go to "Mis podcasts"
- **Error dialog**: Shows error with "Reintentar" / "Cancelar" options
- **Draft save**: Copies audio/cover to app-private `filesDir/drafts/`, saves metadata to SharedPreferences via `DraftsManager`
- **Loading state**: Progress bar + "Subiendo podcast..." text, disables buttons during upload

### fragment_upload.xml (redesigned)
- Cover image card: 160dp rounded corners, centerCrop scaling, placeholder with camera icon
- Title/description: Material TextInputLayout with outline box style (violet accents)
- Category: Card with arrow indicator
- Audio section: Two-card layout (select file + record), audio preview card with full player controls
- Two action buttons: "Publicar podcast" (violet filled) + "Guardar como borrador" (violet outlined)
- Progress overlay with ProgressBar and status text

### SupabaseService.kt
- `Podcast` data class expanded: added `user_id`, `category_id`, `cover_url`, `created_at` fields
- `insertPodcast()`: Inserts into `podcasts` table with `approved=false`
- `uploadCoverImage()`: Uploads image bytes to `pod` bucket
- `getPublicCoverUrl()`: Returns public URL for cover images
- `getPodcastsByUserId()`: Queries podcasts by user_id

### DraftsManager.kt (NEW)
- Local draft storage using SharedPreferences + JSON serialization
- `saveDraft()`: Persists audio file path, cover file path, title, description, category
- `getAllDrafts()`: Returns all saved drafts
- `loadDraft()`: Returns a single draft by ID
- `deleteDraft()`: Removes draft and its associated files from disk

### DraftsFragment.kt + DraftsAdapter.kt (NEW)
- Lists all saved drafts with title, filename
- Tap to open in UploadFragment (loads draft data)
- Delete button with confirmation dialog
- Empty state message when no drafts exist
- Wired to drawer "Borradores" entry in MainActivity

### Nav graph
- `uploadFragment` now has optional `draftId` string argument (nullable)
- `draftsFragment` destination registered

### AndroidManifest.xml
- Added `READ_EXTERNAL_STORAGE` (maxSdkVersion=32)
- Added `READ_MEDIA_IMAGES`
- Added `READ_MEDIA_AUDIO`

### New drawables
- `ic_stop.xml`: Square stop icon for recording
- `ic_save_draft.xml`: Save/floppy disk icon
- `ic_check_circle.xml`: Green checkmark circle
- `ic_error.xml`: Warning/error icon

## Flow summary
1. User fills title, description, selects category, picks cover image, selects/records audio
2. Can preview audio before publishing
3. **"Guardar como borrador"**: Saves locally, navigates to Mis Podcasts. Drafts accessible from drawer.
4. **"Publicar podcast"**: Confirmation dialog → uploads audio + cover to Supabase Storage → inserts record with `approved=false` → success dialog → navigates to "Mis podcasts"
5. Podcast visible in user's profile but audio won't play until admin approves

## Supabase storage buckets required
- `pod` bucket (public): Used for both `audios/{userId}/` and `covers/{userId}/`
