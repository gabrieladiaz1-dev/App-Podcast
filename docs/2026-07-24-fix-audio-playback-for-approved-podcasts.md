# Fix reproducción de podcasts aprobados

## Problema

Cuando un administrador aprobaba un podcast (`approved = true`), el podcast se mostraba a todos los usuarios pero no podía reproducirse ("Error al reproducir").

## Causa raíz

`resolveAudioUrl()` intentaba copiar el archivo de audio del bucket `priv` al bucket `pod` mediante `moveAudioFromPrivToPod()`. Esa copia fallaba (el bucket `pod` está vacío/configurado incorrectamente), pero la función igual retornaba `getPublicAudioUrl("pod", path)` apuntando a un archivo inexistente → el reproductor recibía una URL inválida.

## Cambios realizados

### `UploadFragment.kt` (sin cambios respecto al original)
- Se mantiene la subida al bucket `priv` (funciona correctamente).

### `SupabaseService.kt`

1. **`moveAudioFromPrivToPod()`** — Reemplazada la llamada HTTP raw (`POST /storage/v1/object/copy`) por el API nativo de supabase-kt: `client.storage.from("priv").copy(path, path, destinationBucket = "pod")`. Esto maneja la autenticación correctamente.

2. **`resolveAudioUrl()`** — Para podcasts aprobados:
   - Intenta copiar de `priv` a `pod` usando la nueva implementación
   - Si la copia **falla**, devuelve la URL original almacenada en la BD (desde el bucket `priv`) como fallback
   - Si la copia **funciona**, devuelve la URL pública del bucket `pod`

3. **Limpieza:** Se eliminaron imports y la propiedad `httpClient` que ya no se utilizaban al haber quitado la llamada HTTP raw.

## Archivos modificados
- `app/src/main/java/com/example/audify/SupabaseService.kt`

## Nota sobre Supabase

Para que el flujo funcione correctamente, el bucket `pod` necesita:
1. **Política INSERT** para que la copia desde `priv` pueda crear archivos en `pod`:
   ```sql
   CREATE POLICY "Allow uploads to pod" ON storage.objects
     FOR INSERT TO authenticated
     WITH CHECK (bucket_id = 'pod');
   ```
2. **Política SELECT** para que cualquier usuario pueda leer los archivos aprobados:
   ```sql
   CREATE POLICY "Allow public reads from pod" ON storage.objects
     FOR SELECT USING (bucket_id = 'pod');
   ```
   O simplemente marcar el bucket como **público** en Supabase Dashboard → Storage → `pod` → Configuration → Public bucket.

Si no se configuran estas políticas, la copia fallará y `resolveAudioUrl` usará la URL original del bucket `priv` como fallback. Si el bucket `priv` tiene acceso de lectura público, la reproducción funcionará igualmente.
