# 2026-07-25 - Autor, perfil y filtros de podcasts

## Cambios
- Se habilitó la apertura del perfil del autor desde las tarjetas de podcasts en Inicio, Favoritos, Mis podcasts, Listas y Perfil de usuario.
- Se actualizó el detalle del podcast para que el nombre del autor sea interactivo y abra su perfil público.
- `SupabaseService` ahora resuelve el nombre real de la categoría usando `category_id`, para que el filtrado por categoría funcione con datos reales.
- La pantalla de Inicio ahora conecta la búsqueda por texto y el filtro por categorías sobre la lista cargada.
- Se mantuvo el autor visible en los listados reutilizando el mismo modelo enriquecido con nombre de perfil y categoría.

## Validación
- `:app:assembleDebug` no pudo completarse porque Gradle intentó descargar el toolchain de Java 21 y falló en la resolución del JDK.
- `get_errors` sobre los archivos tocados no reportó errores de compilación o referencias rotas.

## Corrección posterior
- Se detectó que `SupabaseService.kt` había quedado corrupto y había perdido varios métodos públicos usados por las pantallas.
- El archivo se reescribió completo y el build volvió a compilar correctamente usando el JBR de Android Studio en lugar del Java 8 que estaba en `PATH`.
