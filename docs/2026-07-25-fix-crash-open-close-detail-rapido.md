# Fix: crash al abrir/cerrar detalle de podcast muy rapido

Fecha: 2026-07-25

## Problema
La app podia crashear al entrar y salir rapidamente de la pantalla de detalle de podcast.

## Causa tecnica
`DetailFragment` tenia callbacks asíncronos (corrutinas, handler y callbacks del servicio de audio) que seguian intentando actualizar `binding` cuando la vista ya habia sido destruida.

## Cambios aplicados
Archivo: app/src/main/java/com/example/audify/ui/detail/DetailFragment.kt

- Se migraron cargas asíncronas a `viewLifecycleOwner.lifecycleScope` en vez de scope no atado a la vista.
- Se agregaron guardas `if (_binding == null) return` antes de actualizar UI en:
  - corrutinas
  - `Runnable` del seekbar
  - callbacks del servicio (`onPrepared`, `onCompletion`, `onPlayStateChanged`, `onError`)
- Navegacion a perfil de autor ahora usa `view ?: return` en vez de `requireView()`.
- En `onDestroyView()` se limpian callbacks del servicio para evitar emisiones tardias.
- `unbindService` ahora va protegido con `try/catch` para casos de desincronizacion al navegar muy rapido.

## Resultado esperado
Al abrir/cerrar detalle de podcast rapidamente, ya no debe ocurrir crash por acceso a vistas destruidas ni por unbind tardio del servicio.
