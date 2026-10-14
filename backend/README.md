# Pedidos360 - Backend REST API & Mensajeria RabbitMQ (Evaluacion Parcial N° 3)
**Asignatura:** Desarrollo Cloud Native I (DSY1107)  
**Seccion:** 002D  
**Tecnologias:** Java 21 | Spring Boot 3.4 | Spring AMQP (RabbitMQ) | Spring Security | OAuth2 Resource Server (Azure AD) | Spring Data JPA | H2 / PostgreSQL

---

## 1. Como se Enciende el Backend y RabbitMQ? (Guia Paso a Paso)

### Requisitos Previos
1. **Java JDK 21** o superior:
   ```bash
   java -version
   ```
2. **Apache Maven 3.8+**:
   ```bash
   mvn -version
   ```
3. **Docker** (para encender RabbitMQ localmente):
   ```bash
   docker run -d --name rabbitmq-pedidos360 -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```
   - Panel web de RabbitMQ: [http://localhost:15672](http://localhost:15672) (Usuario: `guest`, Password: `guest`).

---

### Paso 1: Abrir la terminal en la carpeta backend
```powershell
cd backend
```

### Paso 2: Ejecutar los tests automatizados (36 tests)
```powershell
mvn test
```
*(Debe reportar `BUILD SUCCESS` con 36 tests aprobados, abarcando seguridad, pedidos, mensajeria, ACK/NACK, reintentos, DLQ y administracion REST).*

### Paso 3: Iniciar el servidor Spring Boot
```powershell
mvn spring-boot:run
```
El servidor estara disponible en el puerto **8080** (`http://localhost:8080`).

---

## 2. Arquitectura de Mensajeria RabbitMQ (EP3)

### Topologia Centralizada (`RabbitMQConstants`)
- **Exchanges:**
  - `pedidos.exchange` (Topic): Exchange principal para publicar eventos de pedidos (`pedido.creado`, `pedido.*`).
  - `pedidos.retry.exchange` (Direct): Exchange para enrutar mensajes a la cola de reintentos con TTL.
  - `pedidos.dlx` (Direct): Dead Letter Exchange para mensajes descartados o con reintentos agotados.
- **Colas:**
  - `pedidos.pedido-creado.queue`: Cola durable principal donde los consumidores procesan los pedidos.
  - `pedidos.pedido-creado.retry.queue`: Cola durable con TTL de 5000 ms y dead-letter routing hacia `pedidos.exchange`.
  - `pedidos.pedido-creado.dlq`: Dead Letter Queue para inspeccion de mensajes fallidos con cabeceras de diagnostico.
  - `pedidos.auditoria.queue`: Cola de auditoria de eventos de negocio.
- **Politica de ACK/NACK, Reintentos y DLQ:**
  - **Confirmacion manual:** Los listeners operan bajo `AcknowledgeMode.MANUAL`.
  - **ACK:** Se confirma tras el procesamiento exitoso con `channel.basicAck(deliveryTag, false)`.
  - **Reintentos:** Ante error recuperable con `x-retry-count < 3`, se incrementa la cabecera y se enruta a `pedidos.retry.exchange` con TTL. Al cumplirse el tiempo, RabbitMQ lo devuelve a la cola principal sin ciclos infinitos.
  - **DLQ:** Al alcanzar `x-retry-count >= 3` o ante eventos invalidos irrecuperables, se envia al DLX con cabeceras `x-exception-message`, `x-exception-type` y `x-timestamp`, confirmando el original para sacarlo de la cola.

---

## 3. Matriz Completa de Endpoints y Seguridad

| Metodo | Ruta | Acceso Requerido | Codigo Esperado | Descripcion |
|---|---|---|---|---|
| `GET` | `/public/health` | Publico (sin token) | `200 OK` | Salud del backend |
| `GET` | `/public/productos` | Publico (sin token) | `200 OK` | Catalogo de productos |
| `GET` | `/public/productos/{id}` | Publico (sin token) | `200 OK` / `404` | Detalle de producto |
| `GET` | `/api/profile` | Autenticado | `200 OK` / `401` | Perfil y roles del JWT |
| `GET` | `/api/pedidos` | Autenticado | `200 OK` / `401` | Historial de compras |
| `POST` | `/api/pedidos` | Scope `recurso.write` | `201 Created` / `403` | Crea pedido y publica en RabbitMQ |
| `GET` | `/api/admin/pedidos` | App Role `ROLE_ADMIN` | `200 OK` / `403` | Metricas y listado de pedidos |
| `DELETE` | `/api/admin/pedidos/{id}` | App Role `ROLE_ADMIN` | `204 No Content` / `403` | Cancelar pedido |
| `GET` | `/api/admin/rabbitmq/status` | App Role `ROLE_ADMIN` | `200 OK` / `403` | Estado y metricas de conexion a RabbitMQ |
| `GET` | `/api/admin/rabbitmq/queues` | App Role `ROLE_ADMIN` | `200 OK` / `403` | Listar todas las colas administradas |
| `GET` | `/api/admin/rabbitmq/queues/{name}` | App Role `ROLE_ADMIN` | `200 OK` / `404` | Detalle y metricas en vivo de cola |
| `POST` | `/api/admin/rabbitmq/queues` | App Role `ROLE_ADMIN` | `201 Created` / `400` | Crear cola (validada con Bean Validation) |
| `DELETE` | `/api/admin/rabbitmq/queues/{name}` | App Role `ROLE_ADMIN` | `204` / `409 Conflict` | Eliminar cola (protege colas criticas) |
| `POST` | `/api/admin/rabbitmq/queues/{name}/purge` | App Role `ROLE_ADMIN` | `200 OK` / `404` | Purgar mensajes de una cola |
| `GET` | `/api/admin/rabbitmq/exchanges` | App Role `ROLE_ADMIN` | `200 OK` / `403` | Listar exchanges administrados |
| `POST` | `/api/admin/rabbitmq/exchanges` | App Role `ROLE_ADMIN` | `201 Created` / `400` | Crear exchange (direct, topic, fanout, headers) |
| `DELETE` | `/api/admin/rabbitmq/exchanges/{name}` | App Role `ROLE_ADMIN` | `204` / `409 Conflict` | Eliminar exchange (protege exchanges criticos) |
| `GET` | `/api/admin/rabbitmq/bindings` | App Role `ROLE_ADMIN` | `200 OK` / `403` | Listar enlaces activos |
| `POST` | `/api/admin/rabbitmq/bindings` | App Role `ROLE_ADMIN` | `201 Created` / `404` | Crear binding (valida existencia previa) |
| `DELETE` | `/api/admin/rabbitmq/bindings` | App Role `ROLE_ADMIN` | `204 No Content` / `409` | Eliminar binding |

---

## 4. Variables de Entorno (12-Factor App)

Toda la configuracion sensible y de infraestructura se parametriza mediante variables de entorno:

| Variable | Descripcion | Valor por Defecto |
|---|---|---|
| `SPRING_RABBITMQ_HOST` | Host del broker RabbitMQ | `localhost` |
| `SPRING_RABBITMQ_PORT` | Puerto de conexion AMQP | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Usuario de RabbitMQ | `guest` |
| `SPRING_RABBITMQ_PASSWORD` | Contrasena de RabbitMQ | `guest` |
| `SPRING_RABBITMQ_VIRTUAL_HOST` | Virtual Host de RabbitMQ | `/` |
| `JWT_ISSUER` | URL del emisor de Microsoft Entra ID | `https://login.microsoftonline.com/common/v2.0` |
| `JWT_AUDIENCE` | Client ID / Audiencia de la API | `api://pedidos360-api` |
| `ALLOWED_ORIGINS` | Origenes habilitados para CORS | `http://localhost:4200,http://localhost:5173` |
| `SPRING_DATASOURCE_URL` | URL JDBC de base de datos | `jdbc:h2:mem:pedidosdb` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena de base de datos | *(vacia)* |

---

## 5. Como Probar los Endpoints de RabbitMQ?

1. Abre el archivo `requests.http` en Visual Studio Code (con la extension **REST Client** instalada).
2. Asigna tu token con rol `ROLE_ADMIN` en la variable `@token`.
3. Haz clic en **Send Request** en cualquiera de las consultas bajo la seccion `5. ADMINISTRACION DE RABBITMQ`.
