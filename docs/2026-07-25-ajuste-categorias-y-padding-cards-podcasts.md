# Ajuste visual en categorias y cards de podcasts propios

Fecha: 2026-07-25

## Objetivo
- Quitar el circulo decorativo en la parte superior de cada card de categoria.
- Reducir el padding superior en las cards de podcasts propios.

## Cambios

### 1) Categoria sin circulo superior
Archivo: app/src/main/res/layout/item_category_grid.xml

- Se elimino el `View` decorativo circular (`bg_circle_small`) que aparecia sobre el nombre de categoria.
- Se retiro el margen superior del titulo de categoria para mantener mejor alineacion vertical.

### 2) Menos padding arriba en card de podcast
Archivo: app/src/main/res/layout/item_podcast.xml

- En el contenedor interno (`ConstraintLayout`) se reemplazo:
  - `android:padding="14dp"`
- Por paddings separados:
  - `android:paddingStart="14dp"`
  - `android:paddingTop="8dp"`
  - `android:paddingEnd="14dp"`
  - `android:paddingBottom="12dp"`

## Resultado esperado
- Cards de categorias mas limpias, sin circulo superior.
- Cards de podcasts propios mas compactas en la parte superior, con menor espacio vacio arriba.
