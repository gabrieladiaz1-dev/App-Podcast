# Fix: crash al cambiar rapido de tabs + estados de carga

Fecha: 2026-07-25

## Problema
Al cambiar muy rapido entre Inicio, Mis podcasts, Favoritos y Perfil, la app podia crashear.
Ademas, no habia estado de carga visible consistente en las 4 pestañas.

## Causa tecnica
- Corrutinas que seguian activas mientras la vista del fragment ya se habia destruido (`_binding = null`).
- Actualizaciones UI con `binding`/`requireView()` fuera de momento seguro.

## Cambios aplicados

### 1) Seguridad de ciclo de vida en fragments
Archivos:
- app/src/main/java/com/example/audify/ui/inicio/InicioFragment.kt
- app/src/main/java/com/example/audify/ui/favorites/FavoritesFragment.kt
- app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt
- app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt

Ajustes:
- Uso de `viewLifecycleOwner.lifecycleScope` para tareas de carga que actualizan UI.
- Guardas `if (_binding == null) return` antes de tocar vistas al volver de corrutinas.
- Navegacion defensiva usando `view ?: return` en lugar de `requireView()` para evitar excepciones si la vista ya no existe.

### 2) Estado de carga en las 4 pestañas
Layouts:
- app/src/main/res/layout/fragment_inicio.xml
- app/src/main/res/layout/fragment_favorites.xml
- app/src/main/res/layout/fragment_podcasts.xml
- app/src/main/res/layout/fragment_profile.xml

Ajustes:
- Se agrego `ProgressBar` centrado en cada pantalla.
- Se muestra/oculta desde cada fragment durante operaciones de carga inicial.

## Resultado esperado
- Se elimina el crash al cambiar rapido entre tabs.
- Inicio, Mis podcasts, Favoritos y Perfil muestran indicador de carga al obtener datos.
