# Estado de carga limpio global en pantallas con carga de datos

Fecha: 2026-07-25

## Objetivo
Aplicar el mismo patron visual de carga "limpio" del perfil al resto de pantallas que cargan datos:
- Mostrar un solo spinner centrado.
- Ocultar contenido principal mientras se carga.
- Restaurar contenido al finalizar.

## Pantallas actualizadas

### Inicio
Archivo: app/src/main/java/com/example/audify/ui/inicio/InicioFragment.kt

- La carga inicial ahora oculta `edtBuscar`, banner, header de destacados y lista.
- Se mantiene comportamiento actual de `SwipeRefresh` para refresco manual (sin doble spinner).

### Favoritos
Archivo: app/src/main/java/com/example/audify/ui/favorites/FavoritesFragment.kt
Archivo layout: app/src/main/res/layout/fragment_favorites.xml

- Se agrego estado de carga con `setContentLoading()`.
- Durante carga se oculta `scrollContent` y se muestra `progressBar`.

### Mis podcasts
Archivo: app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt
Archivo layout: app/src/main/res/layout/fragment_podcasts.xml

- Se agrego `scrollContent` en layout.
- Se implemento contador de cargas concurrentes para coordinar `loadProfile()` y `loadUserPodcasts()` sin parpadeos.
- Spinner visible mientras haya al menos una carga activa.

### Listas
Archivo: app/src/main/java/com/example/audify/ui/lists/ListsFragment.kt
Archivo layout: app/src/main/res/layout/fragment_lists.xml

- Se agregaron `scrollContent` y `progressBar` en layout.
- Se implemento `setContentLoading()` con contador para coordinar carga de listas y podcasts.
- Se migro a `viewLifecycleOwner.lifecycleScope` para seguridad de ciclo de vida.

### Perfil ajeno
Archivo: app/src/main/java/com/example/audify/ui/profile/UserProfileFragment.kt
Archivo layout: app/src/main/res/layout/fragment_user_profile.xml

- Se agrego `scrollContent` en layout.
- Carga de perfil/podcasts ahora oculta contenido y muestra spinner hasta finalizar.

### Borradores
Archivo: app/src/main/java/com/example/audify/ui/drafts/DraftsFragment.kt
Archivo layout: app/src/main/res/layout/fragment_drafts.xml

- Se agrego `progressBar`.
- Carga de borradores con corrutina y estado de carga consistente.

### Detalle de podcast
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt
Archivo layout: app/src/main/res/layout/fragment_detail.xml

- Se agrego `scrollContent` en layout.
- Mientras se obtiene el podcast, se ocultan `layoutCover` y contenido del reproductor, dejando solo spinner.

## Resultado esperado
- Todas las pantallas con carga muestran feedback visual uniforme.
- Se evita mostrar contenido parcial o vacio durante carga.
- Experiencia visual mas limpia y consistente en toda la app.
