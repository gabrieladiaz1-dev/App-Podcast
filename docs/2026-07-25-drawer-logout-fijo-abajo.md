# Drawer: cerrar sesión fijo al fondo con separador

Fecha: 2026-07-25

## Objetivo
Mover `Cerrar sesión` al fondo del menú lateral y dejar el separador justo encima de ese botón.

## Cambios realizados

### Menú lateral
Archivo: app/src/main/res/menu/menu_lateral.xml

- Se eliminaron del menú:
  - item `divider`
  - item `nav_cerrar_sesion`

### Layout del drawer
Archivo: app/src/main/res/layout/activity_main.xml

- Se reemplazó el bloque único de `NavigationView` por un contenedor de drawer (`ConstraintLayout`) con:
  - `NavigationView` ocupando todo el alto.
  - `paddingBottom` para no solapar items con el footer.
  - `View` separador (`viewDrawerDivider`) anclado sobre el footer.
  - `TextView` clickeable `btnDrawerLogout` anclado al fondo con icono de logout.

### Lógica de logout
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- Se removió el caso `nav_cerrar_sesion` del listener de menú.
- Se agregó manejo de logout en `btnDrawerLogout`:
  - limpia sesión
  - ejecuta sign out en Supabase
  - redirige a `LoginActivity` con limpieza de back stack

## Resultado esperado
- En el menú lateral, `Cerrar sesión` siempre aparece al final de la pantalla.
- El separador queda inmediatamente arriba del botón de logout.
