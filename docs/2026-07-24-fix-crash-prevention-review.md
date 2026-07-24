# Revisión de estabilidad: prevención de crashes y ANR

## Problema

La aplicación podía crashear o dejar de responder ("La aplicación no responde") durante el uso. Se realizó una revisión completa de todos los archivos Kotlin para identificar y corregir vulnerabilidades.

## Archivos modificados

### `PodcastAdapter.kt` — CRASH crítico corregido

**Problema:** Se creaban `CoroutineScope(Dispatchers.IO).launch` y `CoroutineScope(Dispatchers.Main).launch` sin ningún control de ciclo de vida. Si el adapter se desadjantaba del RecyclerView (navegación, rotación, etc.), las corrutinas seguían ejecutándose e intentaban actualizar vistas destruidas → crash.

**Solución:**
- Se creó un `adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)` como propiedad del adapter
- Todas las corrutinas (loadFavorites, toggle favorite) usan este scope
- En `onDetachedFromRecyclerView()` se cancelan los hijos del scope: `coroutineContext[Job]?.cancelChildren()`
- Se reemplazaron los `CoroutineScope(Dispatcher.X).launch` anidados por `withContext(Dispatchers.IO)` dentro de un solo `adapterScope.launch`

### `DetailFragment.kt` — Thread safety en sensores

**Problema:** Los callbacks del sensor de proximidad (`onNear`/`onFar`) se ejecutan en el hilo del sensor y llamaban directamente a `service?.setEarpieceMode()`, que modifica `AudioManager` desde un hilo secundario.

**Solución:** Los callbacks del sensor de proximidad ahora se postergan al `handler` (main thread), igual que el shake detector ya hacía.

### `RegisterActivity.kt` — Resultado ignorado

**Problema:** El resultado de `SupabaseService.createProfile()` no se verificaba. Si fallaba, el usuario recibía "Cuenta lista" pero el perfil no se había creado.

**Solución:** Se agregó manejo del resultado con `onFailure` para logging.

## Archivos revisados sin cambios necesarios

- `ListsFragment.kt` — Los `lifecycleScope.launch` dentro de diálogos se cancelan automáticamente al destruirse el fragment
- `ProximitySensorManager.kt` — Null safety correcta con safe calls
- `ShakeDetector.kt` — Correcto
- `AudioForegroundService.kt` — `startForeground()` ya corregido en sesión anterior
- `SupabaseService.kt` — Manejo de excepciones correcto con `wrapJwtError`
- `LoginActivity.kt`, `UploadFragment.kt`, `MainActivity.kt` — Sin issues detectados
- Todos los demás fragments: Manejo correcto de binding y ciclo de vida
