# Subir podcast: header/fondo decorativo se desplaza con el scroll

Fecha: 2026-07-25

## Objetivo
Hacer que el header/fondo circular decorativo de la pantalla Subir podcast se mueva junto al contenido cuando se hace scroll.

## Cambio aplicado
Archivo: app/src/main/res/layout/fragment_upload.xml

- Se elimino la capa decorativa fija en la raiz (`View` con `bg_curve_content`).
- Se cambio fondo de la raiz a blanco.
- Se aplico el gradiente al `ScrollView`:
  - `android:background="@drawable/gradient_bg"`
- Se aplico la curva decorativa al contenedor interno del scroll (`LinearLayout`):
  - `android:background="@drawable/bg_curve_content"`

## Resultado esperado
El header/fondo circular deja de estar fijo y ahora se desplaza junto al contenido al hacer scroll.
