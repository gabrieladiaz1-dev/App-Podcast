# Fix: Mis podcasts mostraba menos items que el contador

Fecha: 2026-07-25

## Problema
En la pantalla Mis podcasts, el contador superior podia mostrar el total (ej. 2) pero la lista renderizar menos items (ej. 1) cuando quedaba activo un filtro previo de categoria o estado.

## Causa
El estado de filtros (`selectedStatus`, `selectedCategory`) podia persistir en el fragment y no siempre quedaba claro visualmente, generando discrepancia entre total y lista visible.

## Solucion aplicada
Archivo: app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt

- Se resetean filtros a estado base al cargar datos:
  - `selectedStatus = StatusFilter.ALL`
  - `selectedCategory = null`
- Se agrego reset automatico en `onResume()` para volver a vista sin filtros al entrar/reingresar a la pantalla.

## Resultado esperado
Al abrir Mis podcasts se muestran todos los podcasts propios de forma consistente con el total informado.
