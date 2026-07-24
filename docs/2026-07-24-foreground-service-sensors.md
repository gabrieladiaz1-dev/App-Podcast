# Foreground Service + Sensores de Proximidad y Giroscopio

**Fecha:** 2026-07-24

## Requisitos implementados

### 1. Foreground Service para reproducción en segundo plano
- `AudioForegroundService` — servicio con `foregroundServiceType="mediaPlayback"`
- El MediaPlayer vive en el servicio, no en el Fragment
- Notificación persistente con controles: retroceder 10s, play/pause, adelantar 10s, detener
- Permisos: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`
- La música continúa al salir de la app

### 2. Sensor de Proximidad (switch auricular/altavoz)
- `ProximitySensorManager` — usa `Sensor.TYPE_PROXIMITY`
- Cerca del oído → `AudioManager.MODE_IN_COMMUNICATION` + auricular
- Lejos del oído → `AudioManager.MODE_NORMAL` + altavoz
- Se activa/desactiva en `onResume`/`onPause` del DetailFragment

### 3. Giroscopio (double shake para play/pause)
- `ShakeDetector` — usa `Sensor.TYPE_GYROSCOPE`
- Detecta dos sacudidas rápidas (< 800ms) con magnitud > 15 rad/s
- Pausa/reanuda el audio sin tocar la pantalla
- Se activa/desactiva en `onResume`/`onPause` del DetailFragment

## Archivos creados/modificados

### Nuevos
- `service/AudioForegroundService.kt` — servicio foreground con MediaPlayer, notificación, earpiece mode
- `service/ProximitySensorManager.kt` — listener de sensor de proximidad
- `service/ShakeDetector.kt` — detector de double shake con giroscopio

### Modificados
- `AndroidManifest.xml` — permisos FOREGROUND_SERVICE, WAKE_LOCK, servicio declarado con `foregroundServiceType="mediaPlayback"`
- `DetailFragment.kt` — reescrito para usar servicio enlazado (bindService) en lugar de MediaPlayer local; sensores enlazados
- `gradle/libs.versions.toml` — agregado `media-compat`
- `app/build.gradle.kts` — agregado `implementation(libs.media.compat)`

## Arquitectura
- `DetailFragment` se enlaza al servicio via `bindService()` + `LocalBinder`
- El fragment controla play/pause/seek a través del servicio
- Los sensores (proximidad + giroscopio) viven en el fragment pero llaman al servicio
- Al salir del fragment, se desenlaza del servicio pero la música sigue (foreground)
- Al volver al fragment, se reenlaza y sincroniza el estado UI
