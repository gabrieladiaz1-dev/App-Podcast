# Filtros en categorias y ajuste de cards en Mis podcasts

Fecha: 2026-07-25

## Objetivo
- Corregir comportamiento de cards de categorias en la pantalla Mis podcasts.
- Permitir filtrar podcasts por estado Aprobados y En revision desde la seccion de categorias.

## Cambios principales

### 1) CategoryAdapter con datos reales y seleccion
Archivo: app/src/main/java/com/example/audify/ui/adapter/CategoryAdapter.kt

- Se elimino la dependencia a MockData.
- Se agrego modelo de UI `CategoryUiItem` con `key`, `title` y `count`.
- Las cards ahora:
  - Muestran conteo real recibido desde el fragment.
  - Tienen estilo visual de seleccion (fondo y borde).
  - Son clicables mediante callback `onCategoryClick`.

### 2) Filtros por estado y categoria en Mis podcasts
Archivo: app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt

- Se agrego estado interno de filtros:
  - `StatusFilter.ALL`
  - `StatusFilter.APPROVED`
  - `StatusFilter.PENDING`
- Se agrego `allPodcasts` y `selectedCategory` para aplicar filtros combinados.
- Se implemento `renderCategoryFiltersAndList()` para:
  - Construir cards de filtro por estado: Todos, Aprobados, En revision.
  - Construir cards de categorias reales segun el estado activo.
  - Filtrar listado de podcasts por estado y categoria seleccionada.
  - Actualizar titulo, vacios y adapters.
- `rvCategories` ahora usa `GridLayoutManager(2)` y `CategoryAdapter`.

## Resultado
- En Mis podcasts ya se puede tocar categorias para filtrar por:
  - Todos
  - Aprobados
  - En revision
- Tambien se puede filtrar por categoria especifica con conteos consistentes.
