# Mis podcasts: quitar etiqueta inferior de categorías en bloque de estados

Fecha: 2026-07-25

## Objetivo
En la pantalla Mis podcasts, remover el texto inferior "Categorías" que aparecía debajo del contador de estados (aprobados/pendientes).

## Cambio aplicado
Archivo: app/src/main/res/layout/fragment_podcasts.xml

- Se eliminó el `TextView` inferior del bloque derecho del header que mostraba:
  - `@string/podcasts_categories_label`

## Resultado esperado
En el bloque de estados de Mis podcasts se muestra solo el valor de aprobados/pendientes, sin la etiqueta inferior de categorías.
