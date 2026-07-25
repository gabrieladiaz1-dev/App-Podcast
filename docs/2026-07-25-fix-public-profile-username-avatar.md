# 2026-07-25 - Fix public profile fallback data

## Cambios
- El perfil público ahora usa `name`, `username` y el fallback del autor del podcast antes de caer en `Usuario`.
- Si existe `avatar_url`, se muestra como imagen; si no, se conserva el avatar por inicial.
- Se amplió el modelo `Profile` para incluir `username`.

## Validación
- `:app:assembleDebug` vuelve a compilar correctamente con el JBR de Android Studio.