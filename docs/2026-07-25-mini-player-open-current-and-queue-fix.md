# Mini reproductor: abrir podcast actual + fix de reproduccion de listas

Fecha: 2026-07-25

## Problemas
1. Al tocar el mini reproductor no se abria el detalle del podcast que estaba sonando.
2. Si ya habia algo reproduciendose y se intentaba reproducir una lista, en algunos casos no arrancaba la lista.

## Cambios aplicados

### 1) Abrir el podcast actual desde mini reproductor
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- `miniPlayerContainer` ahora navega a `detailFragment` del podcast en reproduccion.
- Se evita navegar de nuevo si ya estas en el detalle del mismo `podcastId`.

### 2) Exponer podcast actual desde servicio
Archivo: app/src/main/java/com/example/audify/service/AudioForegroundService.kt

- Se agrego `EXTRA_PODCAST_ID`.
- El servicio ahora guarda `currentPodcastId` y expone `currentPlaybackPodcastId`.
- Se limpia el `currentPodcastId` al detener el servicio.

### 3) Reproduccion de listas mas robusta
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt

- Al iniciar audio, se envia tambien `EXTRA_PODCAST_ID` al servicio.
- En modo cola (lista):
  - si un episodio no tiene audio o no se puede resolver (pendiente), se salta automaticamente al siguiente de la lista.
  - evita que una lista "no arranque" por quedarse en un primer item no reproducible.

## Resultado esperado
- Tocar mini reproductor abre el detalle del podcast actual.
- Reproducir una lista mientras hay audio activo cambia correctamente a la cola de la lista y reproduce el siguiente episodio valido si el primero no aplica.
