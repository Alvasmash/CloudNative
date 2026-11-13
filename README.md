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
- Kafka para herramientas locales: `localhost:9092,localhost:9093`.
- Kafka UI: http://localhost:8081

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
docker compose up -d --wait rabbit1 rabbit2 kafka kafka2 kafka-ui
```

Dentro de `backend`, define `$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093"` y `$env:KAFKA_REPLICAS="2"`, luego ejecuta `mvn spring-boot:run`. Para iniciar también el consumidor en otra terminal, ejecuta `mvn package -DskipTests` y `java -jar target/pedidos360-backend-0.0.1-SNAPSHOT.jar --analytics --spring.kafka.bootstrap-servers=localhost:9092,localhost:9093`.
Dentro de `frontend`: `npm.cmd ci` y `npm.cmd start`.
No ejecutes este modo junto con los contenedores de frontend/backend: usan los mismos puertos. La base nativa se guarda en `backend/data/`; Docker usa `backend_data`. Los pedidos y las vinculaciones de administradores son independientes entre esos dos modos.

## Kafka

Compose levanta dos brokers Kafka 3.9 coordinados por ZooKeeper, Kafka UI y el microservicio Spring Boot de analítica. La API de pedidos y el consumidor de ventas corren en procesos separados. El consumidor se inicia con el mismo JAR en modo `--analytics` y procesa los eventos sin conexión a la base de datos.

| Tópico | Publica | Grupo consumidor | Retención |
| --- | --- | --- | --- |
| `pedidos.eventos` | Creación, cambios de estado y eliminación | `pedidos360-auditoria` | 7 días |
| `pedidos.ventas` | Importe y fecha de cada pedido creado | `pedidos360-ventas` | 30 días |

Cada tópico tiene tres particiones, dos réplicas en Compose y política `delete`. La clave es el ID del pedido y preserva su orden dentro de la partición de cada tópico. Los grupos independientes pueden repartir hasta tres particiones entre instancias. El productor usa JSON, confirmación de las réplicas en sincronía e idempotencia durante los reintentos. Se admite una réplica en sincronía para que el entorno local pueda seguir publicando si se detiene un broker. Los DTO excluyen nombre y correo.

Crea un pedido desde la interfaz como invitado y abre **Kafka UI → Topics** para ver el mismo ID en ambos flujos. **Consumers** permite inspeccionar los grupos, sus particiones y offsets. El consumidor de ventas calcula bruto, neto e IVA y los registra con el identificador y la fecha de la venta. El panel **Administración → Eventos en Kafka** muestra hasta 50 eventos de auditoría recibidos desde que se inició la API.

```powershell
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic pedidos.eventos
docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic pedidos.ventas
docker compose exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 --describe --group pedidos360-ventas
docker compose logs analytics
docker compose exec rabbit1 rabbitmqctl cluster_status
```

La publicación se solicita después del commit. Si Kafka continúa caído tras los reintentos, se registra la publicación fallida y el pedido sigue guardado. Ambos tópicos se publican de forma independiente y no se recuperan desde la base. Los consumidores descartan mensajes inválidos con logs de tópico, partición y offset. Las excepciones de procesamiento se reintentan dos veces. RabbitMQ mantiene los flujos de cocina, notificación y facturación, con sus colas de reintento y DLQ. La notificación y facturación son simulaciones en los logs.

La instalación ZooKeeper usa volúmenes nuevos para los brokers. El volumen Kafka anterior de KRaft se conserva por separado y su historial no se migra al iniciar Compose.

### Administración mediante la API

Las rutas bajo `/api/admin/kafka` requieren un token de administrador de Microsoft Entra ID. Kafka UI ofrece administración local desde el navegador.

| Método y ruta relativa | Operación o cuerpo de ejemplo |
| --- | --- |
| `GET /topics` | Listar tópicos |
| `GET /topics/ep5.demo` | Ver configuración, líderes y réplicas |
| `POST /topics` | `{"name":"ep5.demo","partitions":1,"replicas":2,"retentionMs":86400000,"cleanupPolicy":"delete"}` |
| `PATCH /topics/ep5.demo/configuration` | `{"retentionMs":172800000}` |
| `PUT /topics/ep5.demo/partitions` | `{"total":3}` |
| `DELETE /topics/ep5.demo` | Eliminar un tópico de prueba |
| `GET /groups` | Listar grupos |
| `GET /groups/pedidos360-ventas` | Inspeccionar miembros, asignaciones y offsets |

Para conectar un entorno existente define `APP_KAFKA_ENABLED=true` y `SPRING_KAFKA_BOOTSTRAP_SERVERS`. Ajusta `KAFKA_REPLICAS` al número de brokers disponible antes de crear los tópicos. La conexión cloud a PostgreSQL usa las variables descritas en [el backend](backend/README.md).

## Verificación

Backend: `mvn test`. Frontend: `npm.cmd run build` y `npm.cmd test -- --watch=false --browsers=ChromeHeadless`.
Para usar Edge en Windows: `$env:CHROME_BIN='C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'` antes de los tests.

Los tests cubren las tres cuentas aprobadas, usuarios ordinarios, tokens con firma/emisor/audiencia/tenant incorrectos, correo reciclado con otro `oid`, permisos HTTP, eliminación del login demo, pedidos y mensajería. Los tests criptográficos utilizan claves y un servidor JWKS locales de prueba; nunca una identidad Microsoft simulada en la aplicación desplegada.

Referencias: [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site), [Microsoft MSAL](https://learn.microsoft.com/en-us/entra/msal/javascript/browser/login-user), [Kafka Docker](https://kafka.apache.org/39/getting-started/docker/).
