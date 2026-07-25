# Remover campanita de notificaciones en headers

Fecha: 2026-07-25

## Objetivo
Quitar la campanita de notificaciones del header en las pantallas principales, manteniendo alineado el titulo centrado.

## Cambios aplicados

### Layouts
Se reemplazo el `ImageButton` de notificaciones por un `View` espaciador de 48dp para conservar la simetria del toolbar:

- app/src/main/res/layout/fragment_inicio.xml
- app/src/main/res/layout/fragment_favorites.xml
- app/src/main/res/layout/fragment_podcasts.xml
- app/src/main/res/layout/fragment_profile.xml

### Codigo
Se eliminaron los `setOnClickListener` de `btnNotificacion` porque ya no existe icono interactivo:

- app/src/main/java/com/example/audify/ui/inicio/InicioFragment.kt
- app/src/main/java/com/example/audify/ui/favorites/FavoritesFragment.kt
- app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt
- app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt

## Resultado esperado
La campanita desaparece visualmente de los headers y el titulo queda centrado como antes.
