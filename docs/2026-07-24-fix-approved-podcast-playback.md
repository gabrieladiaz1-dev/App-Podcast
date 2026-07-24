# Fix: Approved podcast cannot be played by other users

## Problem
When a podcast is approved by the admin, it appears in the feed for all users, but only the original uploader can play it. Other users see the playback controls hidden with the message "Este podcast está en revisión y aún no puede escucharse".

## Root cause
In `SupabaseService.resolveAudioUrl()`, when `approved = true`, the method tried to create **signed URLs** via `createSignedUrl()` on buckets `pod` and `priv`. Signed URL creation requires the authenticated user to have permission on the storage object. For users who are **not the file owner**:

1. `createSignedUrl("pod", path)` — fails if the file doesn't exist in `pod` (copy is best-effort) or if the bucket doesn't allow signed URL creation
2. `createSignedUrl("priv", path)` — fails because `priv` bucket RLS restricts access to the file owner

Both attempts fail → returns `null` → `DetailFragment` hides playback controls.

## Fix (v2)
Changed `resolveAudioUrl()` (`SupabaseService.kt:542`) to return the **stored `audio_url` directly** for approved podcasts instead of signed or pod-bucket URLs. The stored URL is `getPublicAudioUrl("priv", path)` — una URL pública al bucket `priv` donde el archivo realmente existe (la subida a `priv` es obligatoria). El bucket `pod` es una copia best-effort que puede fallar.

### Before (original)
```kotlin
if (approved) {
    val buckets = listOf("pod", "priv")
    for (bucket in buckets) {
        try {
            val signed = createSignedUrl(bucket, path)
            return signed
        } catch (e: Exception) { ... }
    }
    return null
}
```

### Before (v1 — aún incorrecto)
```kotlin
if (approved) {
    val podUrl = getPublicAudioUrl("pod", path)
    return podUrl
}
```
Esto mostraba los controles pero el MediaPlayer fallaba porque el archivo no estaba en `pod`.

### After (v2 — definitivo)
```kotlin
if (approved) {
    return audioUrl  // URL pública al bucket priv, donde el archivo sí existe
}
```

## Flujo de audio — resumen
1. **Subida**: archivo se sube a `priv/{userId}/{file}` (obligatorio) y se intenta copiar a `pod/{userId}/{file}` (best-effort)
2. **URL almacenada**: `getPublicAudioUrl("priv", path)` — apunta al bucket `priv`
3. **Reproducción (aprobado)**: se usa la URL almacenada directamente — el archivo existe en `priv`
4. **Reproducción (pendiente)**: se genera signed URL desde `priv` — solo el dueño tiene permiso

## Files changed
- `app/src/main/java/com/example/audify/SupabaseService.kt` — `resolveAudioUrl()` method (lines 542-578)