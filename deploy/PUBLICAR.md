# Publicar registro, productos y personas

Orden: primero actualizar el backend en EC2 y comprobarlo; despues publicar la interfaz en GitHub Pages. Las bases de datos y los brokers existentes se conservan. No se deben subir archivos `.env`, claves privadas, contrasenas ni la carpeta `.local`.

## Backend Ubuntu

La entrega incluye `backend.jar`, `backend.sha256` e `instalar-aws.sh`. Colocalos en una carpeta privada del usuario ubuntu. El instalador comprueba el checksum y que el servicio actual use `/home/ubuntu/pedidos360-backend-0.0.1-SNAPSHOT.jar`.

```bash
bash instalar-aws.sh
```

Pide el Gmail remitente y la clave de aplicacion de 16 caracteres, oculta. Es la misma configuracion usada en local. No pegues la clave en comandos, capturas o chats.

El instalador crea `/etc/pedidos360-public.env` con permisos 600 y un archivo adicional de systemd. Conserva `/etc/pedidos360.env` y `/etc/pedidos360-kafka.env`, que contienen la conexion PostgreSQL y los brokers ya configurados. No reemplaza la base de datos por H2. El servicio sigue usando su configuracion original y el archivo nuevo solo agrega correo, URL de Pages y CORS.

El correo apunta a `https://alvasmash.github.io/CloudNative/#/verificar-correo?token=...`. El servidor usa `FRONTEND_URL=https://alvasmash.github.io/CloudNative/` y `FRONTEND_HASH_ROUTING=true`. En local siguen `http://localhost:4200` y hash desactivado.

Antes del reemplazo guarda el JAR y los archivos que modifica en una carpeta privada `~/pedidos360-release.*`. Si fallan el arranque, CORS o autenticacion SMTP, recupera automaticamente el JAR y la configuracion anteriores. Los cambios de esquema de JPA son aditivos; esa recuperacion no elimina las tablas o columnas nuevas.

## Comprobaciones publicas antes de Pages

```bash
curl -fsS https://jfbxfcaajg.execute-api.us-east-1.amazonaws.com/public/productos
curl -fsS https://jfbxfcaajg.execute-api.us-east-1.amazonaws.com/public/auth/mail-mode
```

La segunda respuesta debe indicar `SMTP`. Comprobar tambien que un usuario anonimo obtiene 401 en `/api/admin/productos` y `/api/admin/usuarios`. Las cuentas por correo usan sesiones bearer del backend; API Gateway debe dejar que Spring valide Authorization y no imponer un autorizador que solo acepte JWT Microsoft.

## Interfaz

Despues de actualizar AWS, subir la entrega a main o ejecutar `Frontend GitHub Pages`. Mantiene la API publica existente, las rutas con hash y el retorno Microsoft `/CloudNative/redirect`. Azure debe conservar esa URL SPA autorizada.

Prueba final: registrar una cuenta, recibir el correo real, verificar desde el enlace de Pages, iniciar sesion, recargar, crear y consultar un pedido, y cerrar sesion. Con Microsoft administrador, comprobar Productos y Personas. Las cuentas antiguas creadas solo en H2 local no se trasladan a PostgreSQL; las cuentas publicas se registran en AWS.
