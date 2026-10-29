# La misma interfaz en GitHub Pages

La versión de Pages utiliza los mismos componentes, estilos, fotos y pantallas que la aplicación local. Solo cambia la configuración de publicación:

- API: `https://jfbxfcaajg.execute-api.us-east-1.amazonaws.com`.
- Base de archivos: `/CloudNative/`.
- Retorno Microsoft: `https://alvasmash.github.io/CloudNative/redirect`, conservando la dirección original del proyecto.
- Rutas con `#` para permitir recargas, por ejemplo `/CloudNative/#/menu`. Los enlaces anteriores como `/CloudNative/menu` se convierten automáticamente.

Para generar los archivos públicos, ejecuta dentro de frontend:

```powershell
npm.cmd ci
npm.cmd run build:pages
```

El resultado está en `frontend/dist/pages/browser`. Su `deployment.json` permite comprobar las direcciones utilizadas. No contiene contraseñas ni datos locales.

El workflow `Frontend GitHub Pages` publica solo la interfaz en gh-pages cuando tú subes cambios a main o lo ejecutas en GitHub. No instala servicios, no se conecta por SSH y no actualiza AWS. La compilación local habitual y los contenedores siguen separados.

La apariencia no depende de otra versión del diseño. Las funciones que requieren servidor dependen de la versión que tengas ejecutándose en AWS: actualmente la API original responde al catálogo, pero todavía no tiene la ruta nueva de invitados. Publicar únicamente archivos web no actualiza ese servidor.

Los archivos locales no cambian el sitio ya publicado hasta que tú los publiques. No se ha ejecutado esa publicación desde esta conversación.

## Perfil de Microsoft compatible con AWS

La interfaz consulta GET /api/profile, disponible en el backend original, y adapta status/user/authorities al perfil visual. Ya no solicita /api/session. Primero valida la sesión con el servidor; no inicia una sesión a partir de un token simplemente decodificado en el navegador.

La interfaz muestra ADMIN solo cuando el servidor lo autoriza y la cuenta pertenece a los tres correos aprobados. La restricción definitiva de esos tres correos debe existir también en el backend; una interfaz no puede reemplazar la autorización del servidor antiguo.
