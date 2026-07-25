# Bottom nav: pill centrada y animacion al seleccionar

Fecha: 2026-07-25

## Objetivo
Mejorar el indicador de item seleccionado del bottom nav para que:
- no quede justo al icono,
- se vea mas largo hacia los lados,
- tenga aire arriba y abajo,
- y tenga animacion al seleccionar.

## Cambios aplicados

### 1) Fondo seleccionado del item
Archivo: app/src/main/res/drawable/bg_bottom_nav_item.xml

- Se ajusto la pill del estado seleccionado:
  - ancho: `46dp`
  - alto: `34dp`
  - radio: `17dp`
  - posicion: centrada horizontal y con `top=4dp`
  - color: `#FFE6E6E6`

Esto deja un fondo mas largo lateralmente y con mejor respiracion vertical alrededor del icono.

### 2) Animacion de seleccion
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- En el `setOnItemSelectedListener` se llama `animateBottomNavSelection(item.itemId)`.
- Se agrego animacion al item seleccionado:
  - arranca en escala `0.94` y alpha `0.92`
  - vuelve a `1f` con `OvershootInterpolator(1.2f)`
  - duracion: `180ms`

## Resultado esperado
El fondo del icono activo se ve centrado, mas ancho y con espacio vertical agradable; ademas, al seleccionar un item se percibe una transicion suave.
