# Sensores ahora viven en AudioForegroundService (funcionan en segundo plano)

**Fecha:** 2026-07-24

## Cambio

Los sensores de proximidad y shake se movieron de `DetailFragment` a `AudioForegroundService` para que sigan funcionando cuando la app está en segundo plano.

## Antes

- `ProximitySensorManager` y `ShakeDetector` se creaban e iniciaban en `onViewCreated`/`onResume` del `DetailFragment`
- Al navegar fuera del fragment o minimizar la app, `onPause` detenía los sensores
- Los callbacks hacían `handler.post` para llamar al servicio desde el fragment

## Ahora

- Los sensores se crean e inician en `AudioForegroundService.startSensors()`
- Se llaman desde `prepareAndPlay()` justo después de que el MediaPlayer está preparado
- Se detienen en `stop()`
- Los callbacks llaman directamente a `setEarpieceMode()` y `togglePlayPause()` dentro del servicio, sin necesidad de postear al handler del fragment

## Archivos modificados

- `service/AudioForegroundService.kt` — agregados `startSensors()`, `stopSensors()`, campos `proximitySensor` y `shakeDetector`
- `ui/detail/DetailFragment.kt` — eliminados `proximitySensor`, `shakeDetector`, `startSensors()`, `stopSensors()`, y referencias en `onResume`/`onPause`/`onDestroyView`
