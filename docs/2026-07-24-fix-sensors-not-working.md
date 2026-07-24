# Fix sensores: proximidad y shake no funcionaban

## Problema

1. **Sensor de proximidad:** Al acercar el teléfono a la oreja, el audio seguía saliendo por el altavoz y la pantalla no se apagaba.
2. **Double shake:** Agitar el teléfono no pausaba el podcast. En algunos dispositivos (Motorola) se activaba la linterna en su lugar.

## Causas

### Proximidad
- `AudioManager.MODE_IN_COMMUNICATION` solo rutea audio de **llamadas**, no audio multimedia. El `MediaPlayer` seguía reproduciendo por el altavoz.
- No se usaba `PROXIMITY_SCREEN_OFF_WAKE_LOCK`, por lo que la pantalla nunca se apagaba al acercar el teléfono.

### Shake
- `ShakeDetector` solo usaba `Sensor.TYPE_GYROSCOPE`. Muchos dispositivos no tienen giroscopio.
- El umbral de 15 rad/s era muy alto.

## Solución

### `AudioForegroundService.kt`
- **`setEarpieceMode()`** ahora usa `MediaPlayer.setPreferredDevice(AudioDeviceInfo)` con `TYPE_BUILTIN_EARPIECE` para rutear el audio multimedia al auricular de llamadas.
- **`acquireProximityWakeLock()`** — Adquiere `PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK` para que la pantalla se apague automáticamente al acercar el teléfono.
- **`releaseProximityWakeLock()`** — Libera el wake lock al alejar el teléfono.
- **`stop()`** — Limpia el wake lock y restablece el device preferido del MediaPlayer a null.

### `ShakeDetector.kt`
- **Fallback:** Si no hay giroscopio, usa `Sensor.TYPE_ACCELEROMETER`.
- **Umbral reducido:** Giroscopio 12 rad/s, Acelerómetro 25 m/s².
- **Mensajes de log** mejorados para facilitar depuración.

## Archivos modificados
- `app/src/main/java/com/example/audify/service/AudioForegroundService.kt`
- `app/src/main/java/com/example/audify/service/ShakeDetector.kt`
