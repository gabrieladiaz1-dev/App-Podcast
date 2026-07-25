# Borradores y Mi perfil: menu hamburguesa y limpieza visual

Fecha: 2026-07-25

## Objetivo
- Reemplazar flecha de atras por menu hamburguesa en Borradores y Mi perfil.
- Quitar header circular/decorativo de la pantalla Borradores.

## Cambios aplicados

### 1) Borradores: toolbar y fondo
Archivo: app/src/main/res/layout/fragment_drafts.xml

- Se cambio el fondo de pantalla a blanco (`@android:color/white`).
- Se eliminaron capas decorativas de header (`bg_header_pattern` y `bg_curve_content`).
- Se reemplazo icono de flecha por hamburguesa:
  - `android:src="@drawable/ic_menu_white"`
  - `contentDescription` a `@string/menu_desc`.
- Se corrigio el espaciador derecho del toolbar a `48dp` de alto para mantener el titulo centrado.

### 2) Borradores: comportamiento del boton
Archivo: app/src/main/java/com/example/audify/ui/drafts/DraftsFragment.kt

- `btnBack` ahora abre el drawer lateral en lugar de hacer `popBackStack()`.

### 3) Mi perfil: toolbar
Archivo: app/src/main/res/layout/fragment_profile.xml

- Se reemplazo flecha por icono hamburguesa en el boton izquierdo.
- Se actualizo `contentDescription` a `@string/menu_desc`.

### 4) Mi perfil: comportamiento del boton
Archivo: app/src/main/java/com/example/audify/ui/profile/ProfileFragment.kt

- Se agrego click en `btnBack` para abrir el drawer lateral.

## Resultado esperado
- En Borradores y Mi perfil se usa menu hamburguesa para navegar al drawer.
- Borradores deja de mostrar header circular/decorativo y queda visualmente limpio.
