# Perfil ajeno: filtros por categorias sin estados

Fecha: 2026-07-25

## Objetivo
- Quitar la seccion de categorias del header en perfil ajeno.
- Reutilizar el esquema de filtros de Mis podcasts, pero solo con categorias (sin Aprobados/En revision).

## Cambios aplicados

### 1) Header simplificado
Archivo: app/src/main/res/layout/fragment_user_profile.xml

- Se elimino del header:
  - divisor vertical de estadisticas
  - bloque de conteo y etiqueta de categorias
- Se mantiene solo el bloque de cantidad de podcasts.

### 2) Filtros por categorias en contenido
Archivo: app/src/main/java/com/example/audify/ui/profile/UserProfileFragment.kt

- Se agrego estado de pantalla:
  - `allPodcasts`
  - `selectedCategory`
- Se implemento `renderCategoryFiltersAndList()` que:
  - Construye cards de filtro con `CategoryAdapter`
  - Incluye opcion `Todos`
  - Agrega categorias reales del usuario con su conteo
  - Filtra la lista de podcasts por categoria seleccionada
  - Actualiza `txtSectionTitle` con el total visible
  - Controla mensajes de vacio para categorias y podcasts

### 3) Estabilidad de lista dentro de scroll
Archivo: app/src/main/java/com/example/audify/ui/profile/UserProfileFragment.kt

- Se usa `NonScrollableLinearLayoutManager` en `rvUserPodcasts` para que el contenedor padre controle el scroll y la lista se expanda completa.
- Se mantiene `isNestedScrollingEnabled = false` y se fuerza `requestLayout()` tras setear adapter.

## Resultado esperado
En perfil ajeno:
- El header ya no muestra categorias.
- Se puede filtrar por categorias con tarjetas tipo selector.
- No aparece filtro por estados Aprobados/En revision.
