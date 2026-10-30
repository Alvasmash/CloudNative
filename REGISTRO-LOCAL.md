# Productos, personas y registro local

Esta entrega se ejecuta en localhost. No modifica AWS ni publica en GitHub.

- Interfaz: http://localhost:4200
- Productos (administrador Microsoft): http://localhost:4200/admin/productos
- Personas (administrador Microsoft): http://localhost:4200/admin/personas
- Registro por correo: http://localhost:4200/registro
- Buzón de pruebas: http://localhost:8025

Los administradores pueden crear, editar y eliminar productos. La eliminación los retira del menú, rechaza compras desde carritos antiguos y conserva los detalles de pedidos históricos. Los cambios de precio tampoco alteran pedidos anteriores. Las imágenes se eligen de la galería local.

El listado de personas tiene búsqueda y páginas de 25 cuentas: nombre, correo, proveedor, rol, verificación, registro y último acceso. Incluye las cuentas por correo y los usuarios Microsoft que ingresen a partir de esta entrega. No descarga el directorio completo de Azure ni incluye visitantes anónimos.

Las cuentas por correo siempre tienen rol USER. Los tres correos administradores reservados siguen accediendo por Microsoft; no pueden convertirse en administradores creando una cuenta local. Las contraseñas se almacenan con BCrypt. La verificación caduca en 24 horas, es de un solo uso y puede reenviarse. No se puede iniciar sesión sin verificar. Las sesiones duran ocho horas y se revocan en el servidor al cerrar sesión.

## Iniciar y probar sin enviar correo real

Desde esta carpeta:

```powershell
docker compose up -d --build
```

Por defecto SMTP apunta a Mailpit, un buzón local. Registra una cuenta, abre http://localhost:8025, abre el enlace y pulsa «Confirmar mi correo». Luego inicia sesión con el correo y contraseña. La pantalla avisa de que el envío está en modo de prueba.

## Activar envío real con Gmail

1. En la cuenta Gmail remitente, activa la verificación en dos pasos.
2. Crea una contraseña de aplicación «Pedidos360» en https://myaccount.google.com/apppasswords. No uses tu contraseña habitual. Algunas cuentas administradas o con Protección Avanzada no admiten esta opción.
3. En PowerShell, desde esta carpeta, ejecuta:

```powershell
powershell -ExecutionPolicy Bypass -File .\configurar-gmail.ps1
```

El script pide el remitente y la contraseña de aplicación de forma oculta. Guarda `.env.gmail` con acceso restringido al usuario de Windows y recrea solo el backend local. El archivo está excluido de Git y de los contextos Docker. No envíes la contraseña por chat ni subas ese archivo.

Después de configurar Gmail, registra una cuenta y revisa la bandeja de entrada o spam. El enlace apunta a localhost:4200 y debe abrirse en este equipo. El registro solo muestra éxito si el servidor SMTP aceptó el envío; esto no garantiza entrega final en la bandeja del destinatario.

Para futuros arranques con Gmail:

```powershell
docker compose --env-file .env.gmail up -d
```

Para volver al buzón local:

```powershell
docker compose up -d --no-deps --force-recreate backend
```

Documentación: [Contraseñas de aplicación de Google](https://support.google.com/accounts/answer/185833?hl=es), [SMTP de Gmail](https://developers.google.com/workspace/gmail/imap/imap-smtp), [correo en Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html), [Mailpit en Docker](https://mailpit.axllent.org/docs/install/docker/).
