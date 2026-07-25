# Ajuste de espaciado vertical en Login y Registro

Fecha: 2026-07-25

## Problema
Habia demasiado espacio entre el header y el inicio del formulario (correo/nombre+correo) en las pantallas de inicio de sesion y registro.

## Cambios realizados

### Login
Archivo: app/src/main/res/layout/activity_login.xml

- `android:paddingTop` del root: `48dp` -> `28dp`
- `headerGuideline`: `0.46` -> `0.40`
- Margen superior de `etEmail`: `40dp` -> `18dp`

### Registro
Archivo: app/src/main/res/layout/activity_register.xml

- `android:paddingTop` del root: `48dp` -> `28dp`
- `headerGuideline`: `0.45` -> `0.39`
- Margen superior de `etFullName`: `36dp` -> `14dp`

## Resultado esperado
El formulario inicia mas cerca del header en ambas pantallas, con una composicion mas compacta y consistente.
