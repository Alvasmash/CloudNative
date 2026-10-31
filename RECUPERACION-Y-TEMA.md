# Recuperar contraseña y tema negro con rosa

## Probar en local

1. Abrir http://localhost:4200/login y pulsar «¿Olvidaste tu contraseña?».
2. Escribir el correo de una cuenta registrada y verificada por correo.
3. Abrir el enlace recibido y escribir dos veces una nueva contraseña de al menos 12 caracteres.
4. Iniciar sesión con la contraseña nueva. La anterior y todas las sesiones previas dejan de servir.

El enlace caduca en 30 minutos, es de un solo uso y no cambia la contraseña hasta enviar el formulario. Solicitar otro correo invalida el enlace anterior. También se envía una notificación después del cambio, sin incluir la contraseña. Las cuentas Microsoft siguen recuperándose con Microsoft.

La respuesta de solicitud es genérica, tanto para cuentas existentes como desconocidas. El envío se procesa en una cola limitada y tiene límites de intentos por correo y conexión. Solo se guardan hashes de los enlaces de recuperación. El cambio y la invalidación de sesiones se confirman en una transacción; las operaciones de cambio e inicio de sesión bloquean la cuenta para evitar carreras.

Si el correo está en modo local, revisar http://localhost:8025. Si está configurado Gmail, revisar entrada y spam. Las pruebas automatizadas de correo utilizan una API y base de datos temporales y Mailpit; no envían mensajes a destinatarios reales ni modifican cuentas de AWS.

## Publicar

Primero instalar el JAR nuevo en AWS con el instalador de la entrega. Reutiliza `/etc/pedidos360-public.env` cuando ya existe, sin pedir nuevamente Gmail. Guarda una copia de recuperación y verifica que `/public/auth/mail-mode` indique `passwordResetAvailable: true`.

Después publicar el frontend en GitHub Pages. En producción el correo abre `/CloudNative/#/restablecer-clave?token=...`; en local utiliza `/restablecer-clave?token=...`. No subir `.env.gmail` ni ningún archivo de claves.

El tema usa variables de `frontend/src/styles.css`: lienzo casi negro, superficies carbón, texto rosa claro y acciones rosas con texto oscuro para mantener el contraste. Se conservan colores diferenciados para errores, avisos y éxito.
