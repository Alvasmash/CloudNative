# Pedidos360 · localhost con acceso Microsoft

Angular 19, Java 21 / Spring Boot, H2 persistente, RabbitMQ de dos nodos y Kafka. La interfaz, la API, la base y los brokers se ejecutan en este equipo. **El inicio de sesión y la validación de identidad usan Microsoft Entra ID por internet**, según la configuración elegida.

## Iniciar y detener

Con Docker Desktop abierto, haz doble clic en `Iniciar-local.cmd` o ejecuta:

```powershell
docker compose up --build -d --wait --wait-timeout 180
```

Abre http://localhost:4200/menu. **Iniciar sesión es opcional**: puedes explorar, confirmar pedidos y seguirlos como invitado. Si quieres entrar con tu cuenta, usa **Continuar con Microsoft**. Para detener, usa `Detener-local.cmd` o `docker compose down`. No uses `down -v` si quieres conservar datos.

Puertos publicados, todos limitados a `127.0.0.1`:

- Interfaz: http://localhost:4200
- API: http://localhost:8080/public/health
- RabbitMQ: http://localhost:15672 y http://localhost:15673 (valores locales de ejemplo: `admin` / `admin123`).
- Kafka para herramientas locales: `localhost:9092`.

## Usuarios y administradores

Los invitados pueden comprar sin cuenta, pero no acceden a administración. Si se inicia sesión, el token de Microsoft determina el perfil: todos reciben **USER**, excepto estas tres cuentas aprobadas:

- `benjaminJerez@pedidos360830.onmicrosoft.com`
- `alva.perezz@duocuc.cl`
- `ne.gonzalezm@duocuc.cl`

La comparación no distingue mayúsculas. El backend valida firma, emisor, audiencia, expiración y tenant del token de acceso. Ignora un rol ADMIN recibido en el navegador o en los claims de roles de una cuenta no aprobada.

El primer inicio válido de cada administrador vincula su correo con el identificador inmutable `oid` del directorio Microsoft, en `admin_identity_bindings`. Esa vinculación se conserva en la base local. Otra cuenta con un `oid` diferente no hereda permisos aunque posteriormente use el mismo correo. Una sustitución legítima de cuenta requiere revisar esa vinculación local con el responsable del sistema. No se admiten tokens locales de demostración ni selección libre de rol.

Los invitados del tenant pueden identificarse con su correo original o con su UPN `#EXT#`; la política reconoce los nombres de invitado correspondientes a los dos correos de Duoc aprobados.

## Comprar sin iniciar sesión

El carrito permite confirmar como invitado con nombre y correo de entrega, sin visitar Microsoft. Ese correo del formulario nunca asigna permisos, aunque coincida con el de un administrador.

El navegador crea una clave aleatoria de 256 bits para esta pestaña y la envía solo a `/public/guest/pedidos` mediante `X-Guest-Token`. El backend guarda su hash SHA-256, no la clave. El historial y seguimiento solo muestran pedidos ligados a esa clave; conocer un número de pedido no permite leerlo. Las respuestas de invitados no se almacenan en caché.

**Conserva esta pestaña para seguir los pedidos de invitado.** Si se pierde el almacenamiento de sesión, no hay recuperación por correo ni asociación automática con una cuenta. Al entrar con Microsoft en la misma pestaña se muestran tanto los pedidos de esa cuenta como los de invitado a los que esta pestaña ya tenía acceso.

Los pedidos autenticados se vinculan al identificador del token, no al correo escrito en el formulario. Otros usuarios no pueden leerlos. Los administradores conservan la vista global. Los pedidos antiguos sin propietario asignado siguen disponibles para administración.

## Registro de Microsoft utilizado

Se recuperó la configuración original del proyecto, y el dominio `pedidos360830.onmicrosoft.com` resuelve al mismo tenant:

- Tenant: `16c2a7cf-f000-475f-a847-f95ee0b36404`.
- Aplicación SPA: `c1ceeea6-1f0a-46ba-b79d-ed659608feef`.
- Audiencia de la API: `aef48164-1df8-49e6-b9f2-c1d8953ea200`.
- Scope delegado: `api://aef48164-1df8-49e6-b9f2-c1d8953ea200/recurso.write`.
- URI de redirección SPA: **`http://localhost:4200/redirect`**.

El registro de Microsoft debe permitir esa redirección como **Single-page application**, tener el permiso delegado de la API y permitir el acceso a las cuentas invitadas. Los dos usuarios de Duoc deben existir como invitados y tener acceso a la aplicación. No se necesita un secreto de cliente en el frontend. La librería MSAL solicita la sesión por redirección completa y el propio retorno `/redirect` procesa la respuesta.

El botón de salir cierra la sesión de Microsoft. Los tokens están gestionados por MSAL en el almacenamiento de sesión de la pestaña y solo se envían a la API local configurada. La API verifica el token en cada solicitud; modificar el perfil del navegador no concede permisos.

Si aparece `AADSTS50011`, revisa la URI exacta en el registro. Si aparece un error de consentimiento, revisa el permiso delegado `recurso.write`. Un inicio de sesión completo depende del tenant y de completar las credenciales/MFA en Microsoft; los tests automatizados no sustituyen ese paso real.

## Versión para GitHub Pages

La interfaz pública usa los mismos componentes, estilos y fotos que la versión local. Se corrigieron la base /CloudNative/, las imágenes, las rutas y el retorno de Microsoft; la API apunta al AWS original. Consulta [la configuración de Pages](frontend/PAGES.md). El flujo de GitHub publica únicamente la interfaz cuando tú subes cambios: no actualiza AWS ni se conecta por SSH.

## Desarrollo nativo

Necesitas Java 21, Maven y Node.js compatible con Angular 19:

```powershell
docker compose up -d rabbit1 rabbit2 kafka
```

Dentro de `backend`: `mvn spring-boot:run`.
Dentro de `frontend`: `npm.cmd ci` y `npm.cmd start`.
No ejecutes este modo junto con los contenedores de frontend/backend: usan los mismos puertos. La base nativa se guarda en `backend/data/`; Docker usa `backend_data`. Los pedidos y las vinculaciones de administradores son independientes entre esos dos modos.

## Kafka

RabbitMQ conserva los procesos de cocina, notificación y facturación al crear un pedido. Kafka registra `PEDIDO_CREADO`, `ESTADO_ACTUALIZADO` y `PEDIDO_ELIMINADO`, sin nombre ni correo del cliente en los eventos.

El broker `pedidos360-local-kafka` funciona en KRaft con un nodo, sin ZooKeeper. El backend usa `kafka:29092` dentro de Docker. El tópico `pedidos.eventos` tiene tres particiones, una réplica y retención de siete días. La clave es el ID del pedido para conservar su orden dentro de una partición. El grupo `pedidos360-auditoria` consume los eventos.

En **Administración → Eventos en Kafka**, **Actualizar eventos** consulta el broker y muestra los últimos 50 eventos recibidos desde que arrancó este backend. Esta lista en memoria se vacía al reiniciar el backend; Kafka conserva sus mensajes y offsets en `kafka_data`.

```powershell
docker exec pedidos360-local-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 --describe --topic pedidos.eventos
docker exec pedidos360-local-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic pedidos.eventos --from-beginning --timeout-ms 10000
```

La publicación se solicita después del commit en H2. La auditoría es de mejor esfuerzo: si Kafka continúa caído tras los reintentos del productor, el fallo se registra y el pedido guardado sigue válido. No hay outbox ni recuperación automática de esos eventos desde la base. Los mensajes corruptos se descartan con advertencia para que el consumidor continúe.

Los consumidores de notificación/facturación simulan esas operaciones en los logs; no envían correos ni emiten documentos fiscales reales.

## Verificación

Backend: `mvn test`. Frontend: `npm.cmd run build` y `npm.cmd test -- --watch=false --browsers=ChromeHeadless`.
Para usar Edge en Windows: `$env:CHROME_BIN='C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'` antes de los tests.

Los tests cubren las tres cuentas aprobadas, usuarios ordinarios, tokens con firma/emisor/audiencia/tenant incorrectos, correo reciclado con otro `oid`, permisos HTTP, eliminación del login demo, pedidos y mensajería. Los tests criptográficos utilizan claves y un servidor JWKS locales de prueba; nunca una identidad Microsoft simulada en la aplicación desplegada.

Referencias: [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site), [Microsoft MSAL](https://learn.microsoft.com/en-us/entra/msal/javascript/browser/login-user), [Kafka Docker](https://kafka.apache.org/39/getting-started/docker/).
