# Subir podcast: menu hamburguesa en toolbar

Fecha: 2026-07-25

## Objetivo
Agregar el menu hamburguesa en la pantalla Subir podcast para abrir el drawer lateral, consistente con otras pantallas principales.

## Cambios aplicados

### Layout
Archivo: app/src/main/res/layout/fragment_upload.xml

- El boton izquierdo del toolbar (`btnBack`) ahora:
  - usa `ic_menu_white`
  - usa `@string/menu_desc`
  - queda visible (se elimino `visibility="gone"`)
- Se corrigio el espaciador derecho del toolbar a alto `48dp` para mantener el titulo centrado.

### Logica
Archivo: app/src/main/java/com/example/audify/ui/upload/UploadFragment.kt

- Se agregaron imports de drawer:
  - `androidx.core.view.GravityCompat`
  - `androidx.drawerlayout.widget.DrawerLayout`
- En `setupListeners()` se agrego click de `btnBack` para abrir el drawer:
  - `drawer.openDrawer(GravityCompat.START)`

## Resultado esperado
En Subir podcast aparece el icono hamburguesa y al tocarlo se abre el menu lateral.
