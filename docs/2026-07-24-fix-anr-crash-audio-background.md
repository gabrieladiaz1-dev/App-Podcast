# Fix ANR/crash y reproducción en segundo plano

## Problema

La aplicación se cerraba inesperadamente después de un tiempo de uso mostrando "La aplicación no responde" (ANR). Esto ocurría especialmente al reproducir audio.

## Causa raíz

`AudioForegroundService` nunca llamaba a `startForeground()`. En Android, cuando se inicia un servicio con `startForegroundService()`, el servicio **debe** llamar a `startForeground()` dentro de los primeros segundos para mostrar una notificación permanente. Si no lo hace, el sistema mata el servicio con el error:

> Context.startForegroundService() did not then call Service.startForeground()

Esto se manifiesta como un ANR para el usuario.

## Cambios en `AudioForegroundService.kt`

### 1. `startForeground()` en `prepareAndPlay()`
Se agregó `startForeground(NOTIFICATION_ID, buildNotification())` justo antes de iniciar la reproducción (`mp.start()`), dentro del callback `setOnPreparedListener`. Esto asegura que el servicio se promueva a foreground **antes** de que el sistema lo mate.

### 2. Variable `hasStartedForeground`
Se agregó un flag para evitar llamar `startForeground()` múltiples veces. También se usa en `onStartCommand()` para el caso en que se recibe un `ACTION_PLAY` con la misma URL (reanudación) y el servicio no se ha puesto en foreground aún.

### 3. `currentUrl` como propiedad de clase
Se mantiene `currentUrl` como propiedad para poder comparar si la URL cambió entre llamadas, permitiendo reusar el MediaPlayer cuando es el mismo audio.

## Archivos modificados
- `app/src/main/java/com/example/audify/service/AudioForegroundService.kt`

## Reproducción en segundo plano

Con esta corrección, el servicio corre correctamente en foreground con una notificación persistente que incluye controles (reproducir/pausar, retroceder, adelantar, detener). El usuario puede:
- Navegar a otras pantallas mientras el audio sigue reproduciéndose
- Cerrar la app y el audio continúa (notificación en la barra de estado)
- Controlar la reproducción desde la notificación
- Detener la reproducción desde la notificación (acción "Detener")
