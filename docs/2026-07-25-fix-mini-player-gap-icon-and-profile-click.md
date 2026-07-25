# Fix: mini reproductor (espacio, icono) y toque en perfil

Fecha: 2026-07-25

## Problemas reportados
- El mini reproductor generaba un espacio visual no deseado en la zona inferior.
- El icono de pausa/play no se veia correctamente.
- En perfil, abrir un podcast podia requerir varios toques.

## Cambios aplicados

### 1) Espacio inferior del mini reproductor
Archivo: app/src/main/res/layout/activity_main.xml

- Se ajusto el constraint del `nav_host_fragment` para terminar en `bottom_navigation` (no en el mini reproductor).
- El mini reproductor queda superpuesto arriba del menu inferior sin reservar altura en el contenido principal.

### 2) Visibilidad del icono pausa/play
Archivos:
- app/src/main/res/layout/activity_main.xml
- app/src/main/java/com/example/audify/MainActivity.kt

- Se aplico `tint` oscuro a iconos de `btnMiniPlayPause` y `btnMiniStop`.
- Se refuerza `imageTintList` en runtime al refrescar estado para asegurar contraste.

### 3) Mejor respuesta al toque en perfil
Archivo: app/src/main/java/com/example/audify/ui/profile/UserProfileFragment.kt

- Navegacion de item y autor cambiada a patron seguro con `val root = view ?: return`.
- Evita fallos/intermitencias de `requireView()` en cambios de estado rapidos.

### 4) Menos "parpadeo" y churn en UI
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- Se redujo frecuencia del refresco del mini reproductor (900ms).
- Solo se actualiza UI cuando cambia visibilidad, titulo o estado play/pause.

## Resultado esperado
- Sin espacio extraño en la parte baja causado por el mini reproductor.
- Icono de pausa/play siempre visible.
- Apertura de podcast desde perfil mas consistente al primer toque.
