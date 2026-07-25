# Detalle: botones nuevos para anterior/siguiente en listas

Fecha: 2026-07-25

## Objetivo
Agregar controles para cambiar de podcast dentro de una lista sin alterar el comportamiento de los botones actuales de seek.

## Cambios realizados

### Layout del detalle
Archivo: app/src/main/res/layout/fragment_detail.xml

- Se agregaron dos botones nuevos debajo de los controles del reproductor:
  - `btnPrevPodcast` (Podcast anterior)
  - `btnNextPodcast` (Podcast siguiente)

### Logica del detalle
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt

- Se restauro el comportamiento original de:
  - `btnRewind` -> retrocede 10 segundos.
  - `btnForward` -> adelanta 10 segundos.
- Los nuevos botones manejan salto de episodio cuando hay cola de lista:
  - `btnPrevPodcast` -> podcast anterior.
  - `btnNextPodcast` -> podcast siguiente.
- Si no se esta reproduciendo una lista, muestran mensaje informativo.

## Resultado esperado
- Los botones existentes mantienen seek ±10s.
- Hay controles separados para navegar episodios de una lista.
