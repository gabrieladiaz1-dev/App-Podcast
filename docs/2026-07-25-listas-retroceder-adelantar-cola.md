# Listas: retroceder y adelantar dentro de la cola

Fecha: 2026-07-25

## Objetivo
Permitir navegar entre podcasts cuando se esta reproduciendo una lista.

## Cambios realizados
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt

- Se detecta modo cola (reproduccion iniciada desde `Listas`) con `queuePodcastIds` + `queueIndex`.
- Boton `btnRewind`:
  - En modo cola: va al podcast anterior.
  - Fuera de cola: mantiene comportamiento actual de retroceder 10 segundos.
- Boton `btnForward`:
  - En modo cola: va al podcast siguiente.
  - Fuera de cola: mantiene comportamiento actual de adelantar 10 segundos.
- Se agrego carga centralizada de episodios de cola (`loadPodcastFromQueue`) para reusar la misma logica y mantener estado de carga limpio.

## Resultado esperado
- Si entras al detalle desde una lista, puedes moverte al podcast anterior/siguiente con los botones del reproductor.
- Si no vienes de una lista, los botones siguen funcionando como seek de ±10s.
