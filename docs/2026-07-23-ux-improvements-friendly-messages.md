# UX Improvements - 2026-07-23

## Categorías: carga bajo demanda
- Ahora las categorías se cargan cuando el usuario toca el card de categoría, no al abrir el fragment
- Se muestra un diálogo de progreso ("Estamos buscando las categorías, un segundito") mientras carga
- Si no hay internet o no se encuentran categorías, se muestra un toast amigable
- Si ya se cargaron antes, se muestra el diálogo inmediatamente sin volver a cargar

## Mensajes amigables en toda la app
Todos los mensajes de error, validación y feedback fueron actualizados a un tono coloquial y respetuoso:

### LoginActivity
- "Ingresa tu correo electrónico" → "Escribe tu correo para poder ingresar"
- "Correo electrónico inválido" → "Ese correo no se ve bien. ¿Lo escribiste completo?"
- "Ingresa tu contraseña" → "¿Y tu contraseña? No la dejaste"
- "Error: [msg]" → "No pudimos ingresar. ¿Correo o contraseña incorrectos?"
- "Ingresando..." → "Entrando..."

### RegisterActivity
- "Ingresa tu nombre completo" → "¿Cómo te llamas? Deja tu nombre completo"
- "Ingresa tu correo electrónico" → "¿Cuál es tu correo? Lo necesitamos para tu cuenta"
- "Ingresa un correo electrónico válido" → "Ese correo no parece válido. ¿Lo escribiste bien?"
- "Crea una contraseña" → "Crea una contraseña para proteger tu cuenta"
- "La contraseña debe tener al menos 6 caracteres" → "Tu contraseña debe tener al menos 6 letras o números"
- "Confirma tu contraseña" → "Escribe tu contraseña otra vez para confirmar"
- "Las contraseñas no coinciden" → "Las contraseñas no coinciden. Revísalas"
- "Cuenta creada exitosamente" → "¡Tu cuenta está lista! Ya puedes ingresar"
- "Error: [msg]" → mensajes específicos según el error (correo ya registrado, contraseña inválida, etc.)

### UploadFragment
- "Selecciona una categoría" → "Selecciona una categoría" (se mantiene)
- "Error: sesión no válida" → "Parece que no has iniciado sesión. Ingresa de nuevo"
- Errores RLS → "No tienes permiso para subir archivos. Revisa la configuración de tu cuenta"
- Timeout → "Se tardó demasiado la conexión. ¿Tienes internet? Intenta de nuevo"
- Host error → "No pudimos conectarnos. Revisa tu internet y vuelve a intentar"
- Error genérico audio → "No pudimos subir el audio. Intenta de nuevo"
- Error genérico insert → "No pudimos guardar tu podcast. Intenta de nuevo"
- Error inesperado → "Algo salió mal inesperadamente. No te preocupes, no se guardó nada raro"
- Diálogo éxito → "¡Listo! Tu podcast ya está en revisión. Pronto podrás verlo en tu perfil"
- Botón error → "Intentar otra vez" (antes "Reintentar")

### ProfileFragment
- "El nombre no puede estar vacío" → "¿Cómo te llamas? Deja tu nombre para guardar"
- "La contraseña debe tener al menos 6 caracteres" → "Tu contraseña nueva debe tener al menos 6 caracteres"
- "Las contraseñas no coinciden" → "Las contraseñas no coinciden. Revísalas"
- "Cambios guardados" → "¡Listo! Tus cambios se guardaron"
- "Error: [msg]" → "No pudimos guardar los cambios. Intenta de nuevo"

### DetailFragment
- "Podcast no encontrado" → "No encontramos ese podcast"
- "Error al cargar audio" → "No pudimos cargar el audio. Intenta de nuevo"
- "Cargando audio..." → "Un segundito, estamos preparando el audio"

### ListsFragment
- "Inicia sesión para crear listas" → "Ingresa para crear listas"
- "Lista [name] creada" → "¡Lista [name] creada!"
- "Escribe un nombre" → "¿Cómo se llama tu lista?"
- "Inicia sesión para modificar listas" → "Ingresa para modificar tus listas"
- "Lista actualizada" → "¡Lista actualizada!"
- "Filtrando: [cat]" → "Mostrando: [cat]"

### PodcastAdapter
- "Inicia sesión para agregar favoritos" → "Ingresa para guardar favoritos"

### UserProfileFragment
- "Usuario no encontrado" → "No encontramos a ese usuario"

### Strings.xml
- "Permiso denegado. Revisa la configuración de la app." → "No nos dieron permiso. Revisa la configuración de tu celular"
- "Error al iniciar la grabación" → "No pudimos empezar a grabar. ¿Tienes permiso del micrófono?"

## Commit
`63a4a5b` — Mejora UX: categorias se cargan al tocar + mensajes amigables en toda la app
