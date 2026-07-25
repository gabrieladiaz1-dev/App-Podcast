# Listas: reproduccion seguida y limpieza visual de tarjeta

Fecha: 2026-07-25

## Objetivo
1. Permitir reproducir una lista completa desde la pantalla de Listas.
2. Hacer que los podcasts de la lista se reproduzcan de corrido (auto siguiente).
3. Quitar la imagen/iconeo del lado derecho en la tarjeta de lista.

## Cambios realizados

### 1) Reproduccion de lista completa
Archivo: app/src/main/java/com/example/audify/ui/lists/ListsFragment.kt

- En el dialogo de detalle de lista se agrego el boton `Reproducir lista`.
- Al pulsarlo, se abre `DetailFragment` con:
  - `podcastId` del primer episodio.
  - `queuePodcastIds` (array de IDs de los podcasts de esa lista).
  - `queueIndex` inicial en `0`.

### 2) Auto avance al siguiente podcast
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt

- Se agrego soporte de cola:
  - lectura de `queuePodcastIds` y `queueIndex` desde argumentos.
  - al terminar un podcast (`onCompletion`), si hay siguiente en cola, se carga automaticamente.
- Si llega al final de la lista, se mantiene comportamiento normal y se muestra `Fin de la lista`.

### 3) Quitar imagen derecha en tarjeta de lista
Archivo: app/src/main/res/layout/item_playlist.xml

- Se quito el icono derecho (`ic_playlist`) de la tarjeta.
- Se mantuvo la accion de borrado con un control de texto `Eliminar` en formato pill.

## Resultado esperado
- Desde Listas se puede iniciar reproduccion de una lista.
- Los podcasts avanzan automaticamente uno tras otro.
- La tarjeta de lista ya no muestra la imagen/icono a la derecha.
