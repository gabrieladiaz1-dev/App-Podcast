# Fix: Audio de podcasts aprobados con signed URLs

**Fecha:** 2026-07-24

## Problema
Los podcasts aprobados de otros usuarios no se reproducían. Solo funcionaban los podcasts propios subidos por el usuario actual.

## Causa
`resolveAudioUrl` usaba `getPublicAudioUrl("pod", path)` que solo construye un string de URL sin validar si el bucket `pod` existía, era público, o si el archivo estaba ahí. MediaPlayer intentaba reproducir una URL que devolvía 404/403 silenciosamente.

## Solución
Cambiar `resolveAudioUrl` para podcasts aprobados a usar `createSignedUrl` en lugar de URLs públicas:

```kotlin
// Antes
val publicUrl = getPublicAudioUrl("pod", path)
return publicUrl  // string sin validación

// Ahora
val signed = createSignedUrl("pod", path)  // llama API real
return signed  // URL temporal verificada
```

Para podcasts aprobados, ahora intenta:
1. `createSignedUrl("pod", path)` — bucket de reproducción
2. `createSignedUrl("priv", path)` — fallback al bucket privado

Las signed URLs son URLs temporales firmadas que:
- Funcionan para cualquier usuario autenticado
- No dependen de que el bucket sea público
- Valida que el archivo exista realmente

## Archivos modificados
- `SupabaseService.kt` — `resolveAudioUrl()`: reemplazado `getPublicAudioUrl` por `createSignedUrl` con fallback entre buckets
