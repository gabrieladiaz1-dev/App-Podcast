# Bottom nav: indicador seleccionado en gris claro

Fecha: 2026-07-25

## Objetivo
Cambiar el fondo oscuro que aparece en el icono seleccionado del menu inferior por un gris claro.

## Cambio aplicado
Archivo: app/src/main/res/layout/activity_main.xml

- En `BottomNavigationView` se agrego:
  - `app:itemBackground="@drawable/bg_bottom_nav_item"`

Archivo: app/src/main/res/drawable/bg_bottom_nav_item.xml

- Fondo por estado del item:
  - Seleccionado: gris claro `#FFE6E6E6`
  - No seleccionado: transparente

Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- Se desactiva el indicador activo interno de Material por codigo (con fallback compatible):
  - `setItemActiveIndicatorEnabled(false)` via reflection cuando la API existe.

## Nota de compatibilidad
Se reemplazo `itemActiveIndicatorColor` porque en esta configuracion de dependencias de Material no existe ese atributo y causaba error de linking de recursos. Ademas, se desactiva el indicador activo por codigo para evitar fondos oscuros residuales entre versiones.

## Resultado esperado
Al seleccionar una opcion del menu inferior, el fondo del icono activo se muestra en gris claro en lugar de gris oscuro.
