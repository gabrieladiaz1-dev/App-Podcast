# Ajuste de loading en Inicio y Mi perfil

Fecha: 2026-07-25

## Objetivo
- Evitar doble indicador de carga en Inicio.
- Mejorar visualmente el estado de carga en Mi perfil.

## Cambios aplicados

### Inicio
Archivo: app/src/main/java/com/example/audify/ui/inicio/InicioFragment.kt

- `loadPodcasts()` ahora recibe `fromSwipeRefresh`.
- Si la carga viene de pull-to-refresh:
  - se muestra solo `SwipeRefresh`.
  - se oculta `ProgressBar`.
- Si la carga es inicial:
  - se muestra solo `ProgressBar`.
  - `SwipeRefresh` no se activa.

Resultado: ya no aparecen dos indicadores de carga al mismo tiempo.

### Mi perfil
Archivos:
- app/src/main/res/layout/fragment_profile.xml
- app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt

Cambios:
- Se agregó `android:id="@+id/scrollContent"` al `ScrollView`.
- Durante carga de `loadUserData()`:
  - `progressBar` visible
  - `scrollContent` invisible
- Al finalizar:
  - `progressBar` oculto
  - `scrollContent` visible

Resultado: estado de carga más limpio, sin superposición visual fea del spinner sobre el contenido.
