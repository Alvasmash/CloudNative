# Publicacion en GitHub Pages

Pages utiliza los mismos componentes, estilos e imagenes que localhost. La configuracion publica se genera sin credenciales:

- API: `https://jfbxfcaajg.execute-api.us-east-1.amazonaws.com`.
- Sitio y base de archivos: `https://alvasmash.github.io/CloudNative/`.
- Retorno Microsoft SPA: `https://alvasmash.github.io/CloudNative/redirect`.
- Rutas con hash para permitir recargas: `#/menu`, `#/registro`, `#/admin/productos`, `#/admin/personas`.

```powershell
npm.cmd ci
npm.cmd run build:pages
```

Resultado: `dist/pages/browser`. `deployment.json` describe las URLs publicas y el modo de rutas. El build normal sigue apuntando al backend local y no usa rutas hash.

El workflow `Frontend GitHub Pages` ejecuta pruebas, compila Pages y publica `gh-pages` al subir cambios a `main` o ejecutarlo manualmente. El sitio usa esa rama como fuente de Pages. No actualiza AWS: primero debe instalarse el backend correspondiente siguiendo `../deploy/PUBLICAR.md`.

El registro requiere que AWS ofrezca `/public/auth/register`, `/public/auth/verify`, `/public/auth/login` y `/api/session`, ademas del correo SMTP configurado. La URL del backend debe ser HTTPS y permitir CORS desde `https://alvasmash.github.io`.

Para los enlaces de correo, AWS necesita `FRONTEND_URL=https://alvasmash.github.io/CloudNative/` y `FRONTEND_HASH_ROUTING=true`. El enlace resultante es `/CloudNative/#/verificar-correo?token=...`. Las credenciales Gmail permanecen en el servidor; no deben incluirse en el frontend ni el repositorio.

Microsoft sigue utilizando `/api/profile` y MSAL. El servidor aplica la regla de los tres correos administradores y la interfaz comprueba el rol devuelto por la API. Las cuentas por correo son siempre usuarios normales. Los visitantes pueden seguir explorando y haciendo pedidos sin registrarse.

Si una API anterior devuelve 404 para Kafka, el panel indica que la funcion no esta disponible. Mostrar esa informacion no instala Kafka; el backend necesita su broker conectado.
