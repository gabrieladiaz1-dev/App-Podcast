# Fix listas: carga y creacion con sesion valida

Fecha: 2026-07-25

## Problema
La pantalla de Listas no cargaba listas y tampoco permitia crearlas en algunos casos.

## Causa principal
La pantalla validaba solo `SessionManager.isLoggedIn()` (estado local), pero no siempre la sesion real de Supabase seguia activa. Cuando la sesion estaba expirada, las operaciones de listas fallaban (RLS/auth) y se mostraba un error generico.

## Cambios aplicados
Archivo: app/src/main/java/com/example/audify/ui/lists/ListsFragment.kt

- Se inicializan `rvPlaylists` y `rvAllPodcasts` con `LinearLayoutManager` al crear la vista.
- Se agrego verificacion de sesion real con Supabase antes de operaciones clave:
  - `loadPlaylists()`
  - `createPlaylist()`
  - `showAddToPlaylistDialog()`
- Se creo helper `ensureListsSessionOrRedirect()`:
  - llama `SupabaseService.ensureValidSession()`
  - si falla, limpia sesion local, avisa al usuario y redirige a login
- Se mejoro feedback de errores:
  - en cargar/crear listas y en agregar/quitar podcasts ahora se muestra `exception.message` cuando existe, en lugar de mensaje fijo.

## Resultado esperado
- Si la sesion esta valida: Listas carga y permite crear/modificar.
- Si la sesion expiro: redirecciona a login con mensaje claro, evitando pantalla rota sin explicacion.
