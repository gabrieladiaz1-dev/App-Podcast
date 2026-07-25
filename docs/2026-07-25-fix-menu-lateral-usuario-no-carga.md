# Fix: usuario no cargaba en el menu lateral

Fecha: 2026-07-25

## Problema
En algunos casos el header del menu lateral no mostraba correctamente nombre/correo del usuario.

## Causa
- El header se cargaba solo una vez en `onCreate()`.
- La sesion remota de Supabase puede tardar en restaurarse, devolviendo datos vacios temporalmente.

## Cambios aplicados
Archivo: app/src/main/java/com/example/audify/MainActivity.kt

- Se agrego fallback local inmediato usando `SessionManager`:
  - nombre desde `SessionManager.getUserEmail()`
  - correo local mientras llega el dato remoto
- Se mantiene actualizacion remota asíncrona desde `SupabaseService`.
- Se recarga header al abrir el drawer (`DrawerListener.onDrawerOpened`).
- Se recarga header en `onResume()` para refrescar al volver a la app.

## Resultado esperado
El usuario en el menu lateral aparece de inmediato con datos locales y luego se corrige con datos remotos cuando están disponibles.
