# Mini reproductor global sobre menu inferior

Fecha: 2026-07-25

## Objetivo
Mostrar un mini reproductor fijo en la parte baja de la app (arriba del bottom navigation) mientras haya reproduccion activa.

## Cambios realizados

### Layout principal
Archivo: app/src/main/res/layout/activity_main.xml

- Se agrego `miniPlayerContainer` (MaterialCardView) entre el contenido y `bottom_navigation`.
- El `nav_host_fragment` ahora termina sobre `miniPlayerContainer` para reservar espacio visual.
- El mini reproductor incluye:
  - `txtMiniNowPlaying`
  - `txtMiniTitle`
  - `btnMiniPlayPause`
  - `btnMiniStop`

### Servicio de audio
Archivo: app/src/main/java/com/example/audify/service/AudioForegroundService.kt

- Se expusieron propiedades de solo lectura para UI global:
  - `currentPlaybackTitle`
  - `hasActivePlaybackSession`

### MainActivity
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- Se agrego conexion al `AudioForegroundService` (`ServiceConnection`) para consultar estado.
- Se agrego updater periodico para refrescar mini reproductor.
- `btnMiniPlayPause` alterna play/pause.
- `btnMiniStop` detiene reproduccion enviando `ACTION_STOP`.
- El mini reproductor se oculta cuando no hay sesion de reproduccion activa.
- Se manejan bind/unbind en ciclo de vida (`onResume`, `onPause`, `onStop`) para evitar leaks.

## Resultado esperado
- Al reproducir un podcast, aparece un mini reproductor encima del menu inferior.
- Permite pausar/reanudar o detener sin entrar al detalle.
- Al detener o finalizar sesion de audio, el mini reproductor desaparece.
