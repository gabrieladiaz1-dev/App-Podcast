# Fix: Botón de favoritos no funciona

## Problema
Al hacer clic en el corazón en las tarjetas de podcast, siempre muestra "No pudimos actualizar el favorito. Intenta de nuevo".

## Causa
1. **`addFavorite` / `removeFavorite` no validaban la sesión** antes de hacer la petición a Supabase.
2. **La tabla `favorites` podría no existir** en Supabase — el SQL anterior solo tenía ALTER TABLE / CREATE POLICY.
3. **`addFavorite` usaba la data class `Favorite`** que enviaba `null` en `id` y `created_at`, lo que puede causar errores de serialización.

## Cambios en código

### `SupabaseService.kt`
- `addFavorite()` ahora recibe `(userId: String, podcastId: String)` directamente y usa `mapOf(...)` para el insert (consistente con otros inserts como `createPlaylist`).
- Antes de insertar, verifica si ya existe el favorito para evitar duplicados.
- `addFavorite()` y `removeFavorite()` verifican `currentSessionOrNull()` antes de ejecutar.

### `PodcastAdapter.kt` / `DetailFragment.kt`
- Se actualiza la llamada a `addFavorite(userId, podcastIdStr)` (ya no usa `Favorite(...)`).
- Se maneja `SessionExpiredException` por separado.

## SQL completo para Supabase

Ejecuta **todo** este SQL en el **SQL Editor** de tu dashboard de Supabase:

```sql
-- 1. Crear la tabla favorites si no existe
CREATE TABLE IF NOT EXISTS favorites (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    podcast_id TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, podcast_id)
);

-- 2. Habilitar RLS
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;

-- 3. Eliminar políticas existentes (si las hay)
DROP POLICY IF EXISTS "Users can insert own favorites" ON favorites;
DROP POLICY IF EXISTS "Users can delete own favorites" ON favorites;
DROP POLICY IF EXISTS "Users can view own favorites" ON favorites;

-- 4. Crear políticas RLS
CREATE POLICY "Users can insert own favorites"
  ON favorites FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own favorites"
  ON favorites FOR DELETE TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Users can view own favorites"
  ON favorites FOR SELECT TO authenticated
  USING (auth.uid() = user_id);
```

## Verificación
1. Ejecuta el SQL completo en Supabase SQL Editor (debe decir "Success")
2. Compila: `.\gradlew.bat :app:assembleDebug`
3. Inicia sesión y prueba favoritos
4. Si falla, revisa logcat con filtro `SupabaseService` para ver el error exacto
