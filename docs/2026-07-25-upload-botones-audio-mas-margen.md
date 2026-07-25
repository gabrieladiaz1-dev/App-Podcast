# Subir podcast: mas margen para botones de audio

Fecha: 2026-07-25

## Objetivo
Dar mas aire lateral y vertical a los botones de audio "Seleccionar archivo" y "Grabar audio".

## Cambio aplicado
Archivo: app/src/main/res/layout/fragment_upload.xml

- En la fila contenedora de botones de audio (`LinearLayout`):
  - `layout_marginHorizontal`: `24dp` -> `30dp`
  - `layout_marginTop`: `8dp` -> `14dp`
  - `layout_marginBottom`: agregado `10dp`
- En el boton `btnRecord`:
  - `layout_marginStart`: `12dp` -> `14dp`

## Resultado esperado
Los dos botones de audio tienen mayor separacion respecto a los lados de la pantalla y mayor respiracion vertical respecto a secciones vecinas.
