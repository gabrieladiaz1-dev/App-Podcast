# Fix de scroll en Mis podcasts: lista cortada en el primer item

Fecha: 2026-07-25

## Problema
La pantalla Mis podcasts podia mostrar el contador correcto (por ejemplo 2), pero visualmente solo se mostraba el primer podcast y el scroll parecia detenerse ahi.

## Causa probable
Combinacion de contenedor de scroll + RecyclerView interno con medicion parcial del alto visible.

## Solucion aplicada

### Layout
Archivo: app/src/main/res/layout/fragment_podcasts.xml

- Se reemplazo `ScrollView` por `androidx.core.widget.NestedScrollView`.
- Se activo `android:fillViewport="true"` para mejorar la medicion del contenido total.

### Fragment
Archivo: app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt

- Se agrego `NonScrollableLinearLayoutManager` para `rvUserPodcasts`:
  - `canScrollVertically()` devuelve `false`.
  - Esto permite que el RecyclerView expanda su alto y delegue el scroll al contenedor padre.
- Se asegura `binding.rvUserPodcasts.isNestedScrollingEnabled = false`.
- Se fuerza `requestLayout()` tras setear adapter para recalcular altura renderizada.

## Resultado esperado
Al tener 2 o mas podcasts propios, la lista ya no se corta en el primero y el scroll permite ver todos los items.
