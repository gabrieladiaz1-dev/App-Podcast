# Ajuste de margen en previsualizacion de imagen de podcast

Fecha: 2026-07-25

## Objetivo
Reducir el margen visual excesivo en la miniatura (preview) de imagen de cada podcast.

## Cambio aplicado
- Archivo: `app/src/main/res/layout/item_podcast.xml`
- Vista modificada: `ivThumbnail`
- Ajuste: `android:padding` paso de `14dp` a `2dp`.

## Resultado esperado
La imagen de portada ocupa casi todo el circulo de previsualizacion y se percibe con margen minimo.
