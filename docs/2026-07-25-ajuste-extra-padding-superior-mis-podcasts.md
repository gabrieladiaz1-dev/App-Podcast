# Ajuste extra de padding superior en Mis podcasts

Fecha: 2026-07-25

## Objetivo
Reducir aun mas el espacio superior de las cards en la pantalla Mis podcasts, sin afectar las cards en otras pantallas (por ejemplo Favoritos).

## Cambios

### 1) Ajuste por contexto en el adapter
Archivo: app/src/main/java/com/example/audify/ui/adapter/PodcastAdapter.kt

- Se agrego parametro opcional `cardTopMarginDp` para controlar margen superior por pantalla.
- Se mantiene `contentTopPaddingDp` para controlar padding interno superior.
- En `bind()` se aplica:
  - padding superior contextual al contenedor interno (`contentContainer`)
  - margen superior contextual a la card raiz (`MaterialCardView`)

### 2) Reduccion solo en Mis podcasts
Archivo: app/src/main/java/com/example/audify/ui/podcasts/PodcastsFragment.kt

- Se actualizo la creacion de `PodcastAdapter` con:
  - `contentTopPaddingDp = 0`
  - `cardTopMarginDp = 2`

## Resultado esperado
En Mis podcasts las cards quedan mas compactas en la parte superior, mientras que en otras pantallas las cards conservan su espaciado por defecto.
