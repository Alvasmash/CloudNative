# Pedidos360 - Arquitectura Cloud Native & Sistema de Mensajería RabbitMQ

**Asignatura:** Desarrollo Cloud Native I (DSY1107)  
**Sección:** 002D  
**Repositorio Oficial:** [https://github.com/Alvasmash/CloudNative](https://github.com/Alvasmash/CloudNative)  
**Despliegue Frontend (GitHub Pages):** [https://alvasmash.github.io/CloudNative/](https://alvasmash.github.io/CloudNative/)  

---

## ⚡ GUÍA RÁPIDA: CÓMO LEVANTAR TODO EL PROYECTO (DE MANERA SIMPLE)

### 📊 Tabla de Accesos y Puertos Rápidos
| Servicio | URL Local | Credenciales / Info |
|---|---|---|
| 🌐 **Frontend Web (SPA Reactiva)** | [http://localhost:4200](http://localhost:4200) | Login con Azure AD o Acceso Rápido |
| ☕ **Backend REST (Spring Boot)** | [http://localhost:8080](http://localhost:8080) | Resource Server OAuth2 / JWT |
| 📦 **Catálogo de Productos (Público)** | [http://localhost:8080/public/productos](http://localhost:8080/public/productos) | Endpoint abierto para pruebas |
| 🐰 **RabbitMQ Consola Web (Nodo 1)** | [http://localhost:15672](http://localhost:15672) | Usuario: `admin` / Clave: `admin123` |
| 🐰 **RabbitMQ Consola Web (Nodo 2)** | [http://localhost:15673](http://localhost:15673) | Usuario: `admin` / Clave: `admin123` |

---

### 📌 Opción 1: Levantar TODO con 1 solo comando (Docker Compose - Recomendado)

Si tienes **Docker Desktop** iniciado, puedes levantar toda la infraestructura (los 2 nodos RabbitMQ en clúster, el backend y el frontend) ejecutando en la raíz del proyecto:

```bash
docker compose up -d
```

#### Para comprobar que todo está corriendo:
```bash
docker compose ps
```

#### Para ver que los 2 nodos de RabbitMQ formaron el Clúster:
```bash
docker exec -it pedidos360-rabbit1 rabbitmqctl cluster_status
```
*(Verás en la salida que ambos nodos `rabbit@rabbit1` y `rabbit@rabbit2` están unidos al mismo clúster).*

#### Para detener todo:
```bash
docker compose down
```

---

### 📌 Opción 2: Levantar en Local por separado (Paso a Paso en 3 terminales)

Si prefieres ejecutar cada componente en tu máquina local sin Docker Compose completo:

#### 1️⃣ Terminal 1: Iniciar RabbitMQ en Docker
```bash
docker run -d --name rabbitmq-pedidos360 -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```
* Panel web: [http://localhost:15672](http://localhost:15672) (Usuario: `guest` / Clave: `guest`)

#### 2️⃣ Terminal 2: Iniciar el Backend (Java Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
* Estará disponible en [http://localhost:8080](http://localhost:8080).
* Para correr las 41 pruebas unitarias e integración: `mvn clean test`.

#### 3️⃣ Terminal 3: Iniciar el Frontend Web (SPA Reactiva)
```bash
cd frontend
npm install
npm start
```
> [!TIP]
> **En Windows PowerShell:** Si ves el error *"la ejecución de scripts está deshabilitada en este sistema"*, ejecuta simplemente:
> ```bash
> cmd.exe /c npm start
> ```
* Estará disponible en tu navegador en [http://localhost:4200](http://localhost:4200).

---

## 💻 EL FRONTEND WEB (SPA REACTIVA: ANGULAR CON SIGNALS / "REACTIVO")

El cliente web de **Pedidos360** está ubicado en la carpeta `/frontend` y fue diseñado bajo el paradigma de **Frontend Reactivo Moderno**:

### ¿Cómo funciona la Reactividad en el Frontend?
* **Arquitectura de Signals (Angular 19 Standalone):** En lugar de recargar páginas o realizar sondeos manuales, la aplicación maneja el estado de manera 100% reactiva mediante *Signals*:
  * `CartService`: Carrito reactivo que recalcula en tiempo real precios, impuestos en CLP y límites de stock.
  * `AuthService`: Gestión reactiva de tokens JWT, roles (ADMIN / CLIENTE) y estado de sesión.
* **Componentes Standalone Desacoplados:** Sin módulos pesados, arranque ultrarrápido y carga diferida (*lazy loading*).
* **Compilación de Producción:**
  ```bash
  cd frontend
  npm run build
  ```
  Genera el paquete optimizado en `frontend/dist/frontend`.

---

## 🔑 CÓMO INICIAR SESIÓN (SIN ERRORES)

En la pantalla de bienvenida ([http://localhost:4200](http://localhost:4200)) tienes 2 opciones de acceso:

1. **Opción A (Microsoft Azure AD / Entra ID Oficial):**  
   Haz clic en el botón azul **"Iniciar sesión con Microsoft"**. Se abrirá la pasarela oficial de Microsoft.  
   *(El error `AADSTS50011` reportado inicialmente ya está 100% corregido mediante la resolución dinámica de URIs tanto para localhost `http://localhost:4200/redirect` como para GitHub Pages `https://alvasmash.github.io/CloudNative/redirect`).*

2. **Opción B (Acceso Rápido / Evaluación Académica):**  
   Si estás evaluando el proyecto y no tienes a mano una cuenta del tenant institucional de Microsoft, haz clic en **"Entrar como ADMIN"** o **"Entrar como Cliente"**.  
   * Entrarás de inmediato con el rol asignado, permitiéndote navegar por el Menú, agregar al Carrito, confirmar pedidos e interactuar con el panel de administración sin bloqueos.

---

## 🐰 RABBITMQ: LAS 3 COLAS, DLQ Y EL CLÚSTER (EXPLICADO EN SIMPLE)

### 1. ¿Cuáles son las 3 Colas Funcionales?
Cuando un cliente hace un pedido, el sistema publica eventos asíncronos en paralelo hacia 3 colas de negocio distintas:

1. 🍳 **Cola 1 (Cocina / Preparación):** `pedidos.pedido-creado.queue`  
   * **Consumidor:** `PedidoConsumer`  
   * **Función:** Notifica a la cocina para que comience a elaborar los platos.  
   * **DLQ si falla:** `pedidos.pedido-creado.dlq`

2. 📱 **Cola 2 (Notificaciones al Cliente):** `pedidos.notificacion.queue`  
   * **Consumidor:** `NotificacionConsumer`  
   * **Función:** Envía mensaje/correo push al cliente informando que su pedido fue recibido.  
   * **DLQ si falla:** `pedidos.notificacion.dlq`

3. 🧾 **Cola 3 (Facturación y Boleta Fiscal DTE):** `pedidos.facturacion.queue`  
   * **Consumidor:** `FacturacionConsumer`  
   * **Función:** Genera el folio tributario electrónico y emite la boleta del pedido.  
   * **DLQ si falla:** `pedidos.facturacion.dlq`

4. 📋 **Cola Extra (Auditoría Global):** `pedidos.auditoria.queue`  
   * Escucha todos los eventos del sistema (`pedido.*`) para trazabilidad y auditoría.

---

### 2. ¿Qué es una DLQ (Dead Letter Queue) y cómo funciona aquí?
Una **DLQ (Cola de Mensajes Muertos)** es un buzón especial donde van a parar los mensajes que fallan reiteradamente o tienen datos corruptos, evitando que el sistema colapse o se quede en un bucle infinito.

* **Confirmación Manual (ACK/NACK):** Ningún mensaje se elimina de la cola hasta que el consumidor confirme que se procesó con éxito (`channel.basicAck`).
* **Reintentos Automáticos con TTL:** Si ocurre un fallo recuperable, el mensaje va a una cola de reintento con un TTL de 5 segundos. Se reintenta hasta 3 veces incrementando la cabecera `x-retry-count`.
* **Derivación a la DLQ:** Si falla 3 veces o el mensaje viene corrupto, se deriva a la DLQ correspondiente (`pedidos.*.dlq`) con cabeceras de diagnóstico:
  * `x-retry-count`: Número de intentos realizados.
  * `x-exception-message`: Motivo exacto del fallo.
  * `x-original-queue`: Cola donde ocurrió el problema.
  * `x-timestamp`: Fecha y hora del descarte.

---

### 3. ¿Cómo probar la emisión y consumo de mensajes?
1. Abre el frontend en [http://localhost:4200](http://localhost:4200) e inicia sesión.
2. Ve a la pestaña **Menú**, agrega cualquier producto (ej. Pizza Margherita) y presiona **Confirmar Pedido**.
3. Mira la terminal donde corre Spring Boot: verás los logs inmediatos:
   ```text
   [INFO] Evento publicado a exchange 'pedidos.exchange' con routing key 'pedido.creado'
   [INFO] Evento publicado a exchange 'pedidos.exchange' con routing key 'pedido.notificacion'
   [INFO] Evento publicado a exchange 'pedidos.exchange' con routing key 'pedido.facturacion'
   [INFO] [PedidoConsumer] Procesando pedido ID: 1 - ACK confirmado
   [INFO] [NotificacionConsumer] Enviando confirmación al cliente - ACK confirmado
   [INFO] [FacturacionConsumer] Boleta DTE generada exitosamente - ACK confirmado
   ```
4. También puedes abrir el panel de RabbitMQ ([http://localhost:15672](http://localhost:15672)) y ver las gráficas de mensajes en tiempo real.

---

### 4. ¿Cómo probar el Administrador REST de RabbitMQ?
El backend incluye un controlador REST para administrar el broker directamente vía HTTP (archivo listo en `backend/requests.http`):

* **Ver estado de conexión del broker:**
  ```bash
  curl -X GET http://localhost:8080/api/admin/rabbitmq/status
  ```
* **Listar colas activas y número de mensajes:**
  ```bash
  curl -X GET http://localhost:8080/api/admin/rabbitmq/queues
  ```
* **Crear una cola dinámica de prueba:**
  ```bash
  curl -X POST http://localhost:8080/api/admin/rabbitmq/queues \
    -H "Content-Type: application/json" \
    -d '{"name": "cola.prueba.temporal", "durable": true, "autoDelete": false}'
  ```
* **Intentar borrar una cola crítica (Protección del sistema):**
  ```bash
  curl -X DELETE http://localhost:8080/api/admin/rabbitmq/queues/pedidos.pedido-creado.queue
  ```
  *(El sistema responderá con un error controlado impidiendo borrar las colas críticas).*

---

## 1. Resumen Ejecutivo y Arquitectura

Pedidos360 es una plataforma gastronómica distribuida y reactiva diseñada bajo los principios **Cloud Native**:
- **Frontend SPA:** Angular 19 con componentes Standalone, Signals reactivos y Microsoft Authentication Library (MSAL) para OpenID Connect / OAuth 2.0.
- **Backend Resource Server:** Java 21 y Spring Boot 3.4.3 con Spring Security OAuth2, validación criptográfica de tokens JWT (Microsoft Entra ID), y Spring Data JPA.
- **Mensajería Asíncrona:** Clúster de RabbitMQ con 2 nodos interconectados, exchanges de tipo `Topic` y `Direct`, 3 colas funcionales desacopladas, cada una con su respectiva **Dead Letter Queue (DLQ)**, política de confirmación manual (**ACK/NACK**), reintentos con TTL y microservicio REST administrador de mensajería.
- **Persistencia Cloud:** Base de datos relacional compatible con PostgreSQL / Supabase y AWS RDS, con soporte local en memoria (H2) para testing ultrarrápido y reproducible.
- **Contenedorización & Orquestación:** `docker-compose.yml` integral con formación automática del clúster RabbitMQ, healthchecks y multi-stage builds.
- **API Manager & Cloud Gateway:** Enrutamiento centralizado y validación perimetral de JWT mediante AWS API Gateway / Cloud API Management.

```
                              +---------------------------------------+
                              |      Microsoft Entra ID (Azure AD)    |
                              |   Tenant: 16c2a7cf-... (OAuth 2.0)    |
                              +-------------------+-------------------+
                                                  ^
                               (Login / PKCE / JWT)
                                                  |
+---------------------+     HTTPS      +----------+----------+      CORS / JWT     +-------------------------+
|   Angular 19 SPA    | ------------> |    API Manager Cloud | -----------------> | Spring Boot Backend     |
| (GitHub Pages /     |                |  (AWS API Gateway)  |                    | (OAuth2 Resource Server)|
|  Docker Nginx:4200) |                +---------------------+                    +------------+------------+
+---------------------+                                                                        |
                                                                                    (Spring AMQP / Topic)
                                                                                               |
                                             +-------------------------------------------------+
                                             |
                         +-------------------+--------------------+
                         |             Clúster RabbitMQ           |
                         |  Node 1: rabbit@rabbit1 (Port 5672)    |
                         |  Node 2: rabbit@rabbit2 (Port 5673)    |
                         +-------------------+--------------------+
                                             |
                   +-------------------------+-------------------------+
                   |                         |                         |
        [Cola 1: Pedido Creado]   [Cola 2: Notificaciones]   [Cola 3: Facturación]
        pedidos.pedido-creado.queue pedidos.notificacion.queue pedidos.facturacion.queue
                   |                         |                         |
               (Fallos)                  (Fallos)                  (Fallos)
                   v                         v                         v
        pedidos.pedido-creado.dlq   pedidos.notificacion.dlq   pedidos.facturacion.dlq
```

---

## 2. Tecnologías y Versiones Utilizadas

| Componente | Tecnología | Versión | Propósito |
|---|---|---|---|
| **Lenguaje Backend** | Java OpenJDK | 21 LTS | Runtime principal del backend |
| **Framework Backend** | Spring Boot | 3.4.3 | Microservicio REST & Resource Server |
| **Seguridad Backend** | Spring Security OAuth2 | 6.4.3 | Validación JWT y control RBAC |
| **Mensajería** | Spring AMQP / RabbitMQ | 3.13-management | Procesamiento asíncrono y clúster |
| **Persistencia** | Spring Data JPA / Hibernate | 6.6.8 | Capa de datos ORM transaccional |
| **Base de Datos** | H2 (Local) / PostgreSQL Driver | 2.3 / 42.7 | Persistencia local y Supabase/RDS |
| **Pruebas Backend** | JUnit 5 / Mockito / MockMvc | 5.11 / 5.14 | 41 pruebas automatizadas unitarias/integración |
| **Frontend Framework** | Angular | 19.2.1 | Single-Page Application (SPA) con Signals |
| **Autenticación Frontend** | @azure/msal-browser & msal-angular | 3.28 / 3.0 | OAuth 2.0 PKCE & OpenID Connect |
| **Servidor Web SPA** | Nginx Alpine | 1.27 | Contenedor de producción para Angular |
| **Contenedores** | Docker & Docker Compose | 29.7+ / 3.8 spec | Multi-stage builds y orquestación local |

---

## 3. Solución al Error de Inicio de Sesión (Microsoft Entra ID / AADSTS50011)

### Diagnóstico de la Causa Raíz
El error reportado por Microsoft:
```text
AADSTS50011: The redirect URI 'https://alvasmash.github.io/CloudeNative/redirect' specified in the request
does not match the redirect URIs configured for the application 'c1ceeea6-1f0a-46ba-b79d-ed659608feef'.
```
Se originaba por una discrepancia tipográfica:
1. En `frontend/src/environments/environment.prod.ts` la URI estaba escrita con una `e` adicional: `CloudeNative` en vez de `CloudNative`.
2. En `.github/workflows/frontend-deploy.yml` el flag `--base-href` también contenía `/CloudeNative/`.
3. El registro de la aplicación en el Portal de Azure tiene registradas las URIs oficiales del repositorio sin dicha errata.

### Correcciones Implementadas
1. **Corrección de `environment.prod.ts`:**
   `redirectUri: 'https://alvasmash.github.io/CloudNative/redirect'`
2. **Corrección de `.github/workflows/frontend-deploy.yml`:**
   `npm run build -- --base-href /CloudNative/`
3. **Resolución Dinámica en `msal.config.ts` (`resolveRedirectUri()`):**
   Permite que la aplicación detecte automáticamente si se ejecuta en `localhost` (`http://localhost:4200/redirect`) o en la nube (`https://alvasmash.github.io/CloudNative/redirect`), garantizando funcionamiento idéntico en ambos entornos.
4. **URIs configuradas en Azure Portal (App `c1ceeea6-1f0a-46ba-b79d-ed659608feef`):**
   - `http://localhost:4200/redirect` (Desarrollo local)
   - `https://alvasmash.github.io/CloudNative/redirect` (Producción GitHub Pages)
   - `http://localhost:4200/` y `https://alvasmash.github.io/CloudNative/` (Post-logout)

---

## 4. Implementación Completa de RabbitMQ (EP3 & EP4)

### 4.1. Topología Centralizada
Toda la configuración está centralizada en `RabbitMQConstants.java` y `RabbitMQConfig.java`, eliminando cadenas mágicas:

- **Exchanges:**
  - `pedidos.exchange` (**TopicExchange**, durable): Distribuye eventos de pedidos según la clave (`pedido.creado`, `pedido.notificacion`, `pedido.facturacion`, `pedido.*`).
  - `pedidos.direct.exchange` (**DirectExchange**, durable): Para comandos directos punto a punto.
  - `pedidos.retry.exchange` (**DirectExchange**, durable): Enruta a la cola de reintentos con TTL.
  - `pedidos.dlx` (**DirectExchange**, durable): Dead Letter Exchange para mensajes rechazados.

### 4.2. Las Tres Colas Funcionales con sus DLQ Dedicadas

| Dominio Funcional | Cola Principal | Routing Key | Dead Letter Queue (DLQ) | DLQ Routing Key | Consumidor |
|---|---|---|---|---|---|
| **1. Cocina / Preparación** | `pedidos.pedido-creado.queue` | `pedido.creado` | `pedidos.pedido-creado.dlq` | `pedido.dlq` | `PedidoConsumer` |
| **2. Notificaciones Cliente** | `pedidos.notificacion.queue` | `pedido.notificacion` | `pedidos.notificacion.dlq` | `notificacion.dlq` | `NotificacionConsumer` |
| **3. Boleta Fiscal (DTE)** | `pedidos.facturacion.queue` | `pedido.facturacion` | `pedidos.facturacion.dlq` | `facturacion.dlq` | `FacturacionConsumer` |
| **Auditoría Global** | `pedidos.auditoria.queue` | `pedido.*` | - | - | `AuditoriaConsumer` |
| **Reintentos Temporizados**| `pedidos.pedido-creado.retry.queue`| `pedido.reintento`| - | `pedido.creado` | RabbitMQ (TTL 5000ms) |

### 4.3. Estrategia de Confirmación: ACK, NACK, Reintentos y DLQ
- **Modo Manual:** Configurado con `AcknowledgeMode.MANUAL` en el contenedor de listeners.
- **Procesamiento Exitoso:** Se emite `channel.basicAck(deliveryTag, false)` al completar las operaciones.
- **Errores Recuperables (Transient Errors):** Se incrementa la cabecera `x-retry-count`. Si `x-retry-count < 3`, el mensaje se publica en `pedidos.retry.exchange` con un TTL de 5.000 ms. Al expirar el TTL, RabbitMQ lo reinyecta automáticamente al exchange principal.
- **Errores No Recuperables (Corrupción / Límite de Reintentos):** Se deriva hacia `pedidos.dlx` con cabeceras de diagnóstico completas:
  - `x-retry-count`, `x-max-retries`, `x-exception-message`, `x-exception-type`, `x-original-queue`, `x-timestamp`.
- Se emite `basicAck` sobre el mensaje original para evitar bucles infinitos de reencolamiento.

### 4.4. Microservicio Administrador de RabbitMQ (`RabbitMQAdminController`)
Expone una API REST para gestionar colas, exchanges y enlaces:

- `GET /api/admin/rabbitmq/status`: Estado de conexión con el broker, host, puerto y conteo de recursos.
- `GET /api/admin/rabbitmq/queues`: Lista todas las colas administradas con sus métricas.
- `GET /api/admin/rabbitmq/queues/{name}`: Detalle de mensajes activos y consumidores de una cola.
- `POST /api/admin/rabbitmq/queues`: Crea una cola validando nombre y atributos (`@Valid QueueRequestDTO`).
- `DELETE /api/admin/rabbitmq/queues/{name}`: Elimina una cola. **Protege las colas críticas del sistema**.
- `GET /api/admin/rabbitmq/exchanges`: Lista los exchanges registrados.
- `POST /api/admin/rabbitmq/exchanges`: Crea un exchange (`direct`, `topic`, `fanout`, `headers`).
- `DELETE /api/admin/rabbitmq/exchanges/{name}`: Elimina un exchange con protección de elementos críticos.
- `POST /api/admin/rabbitmq/bindings`: Crea un binding validando existencia previa de la cola y del exchange.
- `DELETE /api/admin/rabbitmq/bindings`: Elimina un enlace existente.

---

## 5. Docker Compose & Clúster RabbitMQ de 2 Nodos

El archivo `docker-compose.yml` en la raíz define la infraestructura completa:

### Configuración del Clúster:
- **Nodo 1 (`rabbit1`):** Hostname `rabbit1`, puertos `5672:5672` y `15672:15672`.
- **Nodo 2 (`rabbit2`):** Hostname `rabbit2`, puertos `5673:5672` y `15673:15672`.
- **Erlang Cookie compartida:** `RABBITMQ_ERLANG_COOKIE: PEDIDOS360_SECRET_COOKIE_2026`.
- **Mecanismo de Descubrimiento:** `rabbit_peer_discovery_classic_config` configurado en `docker/rabbitmq/rabbitmq-node1.conf` y `rabbitmq-node2.conf`.
- **Particionamiento de Red:** `cluster_partition_handling = autoheal` para recuperación automática ante particiones de red.

### Comandos de Ejecución y Verificación:

```bash
# Iniciar toda la infraestructura en segundo plano
docker compose up -d

# Verificar el estado del clúster de RabbitMQ (demuestra que ambos nodos están unidos)
docker exec -it pedidos360-rabbit1 rabbitmqctl cluster_status
```

Consolas de administración web:
- **Nodo 1:** [http://localhost:15672](http://localhost:15672) (Usuario: `admin`, Clave: `admin123`)
- **Nodo 2:** [http://localhost:15673](http://localhost:15673) (Usuario: `admin`, Clave: `admin123`)

---

## 6. Resultados de Compilación y Pruebas Automatizadas

### Backend (JUnit 5 & Spring Boot Test)
Ejecutado con `mvn clean test`:
```text
[INFO] Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  01:13 min
[INFO] ------------------------------------------------------------------------
```
**Desglose de pruebas aprobadas:**
1. `Pedidos360SecurityAndApiTests` (15 tests): Endpoints públicos, 401 sin token, 403 sin rol ADMIN, endpoints autenticados y catálogo de productos.
2. `PedidoConsumerAckNackAndDlqTest` (4 tests): Flujo de ACK manual, reintentos con incremento de `x-retry-count`, descarte a DLQ sin bucles.
3. `NotificacionAndFacturacionConsumerTest` (5 tests): Validación y ACK de notificaciones y boletas fiscales DTE, detección de datos corruptos y derivación a sus DLQ correspondientes.
4. `PedidoEventProducerAndServiceTest` (2 tests): Creación transaccional de pedidos y emisión simultánea hacia las 3 colas de negocio.
5. `RabbitMQAdminServiceTest` (8 tests): Creación y eliminación de colas/exchanges/bindings, protección de colas críticas y estado del broker.
6. `RabbitMQAdminControllerSecurityAndApiTests` (12 tests): Seguridad RBAC, validación `@Valid` de DTOs, códigos HTTP semánticos (200, 201, 204, 400, 401, 403, 404).

### Frontend (Angular 19)
Ejecutado con `npm run build`:
```text
Application bundle generation complete. [17.974 seconds]
Output location: dist/frontend
```
Compilación sin advertencias ni errores.

---

## 7. Guía para Demostración Académica (5 a 10 Minutos)

Para presentar la evaluación ante el docente o evaluador:

### Minuto 0 a 2: Arquitectura y Frontend Web Reactivo
1. Abrir `http://localhost:4200` (o la versión cloud en GitHub Pages).
2. Mostrar la pantalla de inicio de sesión:
   - Demostrar el botón **"Iniciar sesión con Microsoft"** que utiliza MSAL y redirige al Tenant de Microsoft Entra ID sin errores de redirect URI.
   - Demostrar el modo **"Evaluación Académica"** con selección rápida de roles (ADMIN / CLIENTE) para auditoría de rutas protegidas sin requerir cuenta institucional externa.
3. Ingresar al catálogo, agregar productos al carrito y verificar el comportamiento reactivo inmediato de totales e impuestos.

### Minuto 2 a 5: Mensajería RabbitMQ & 3 Colas con DLQ
1. Abrir la consola de RabbitMQ en `http://localhost:15672` (o consultar `GET /api/admin/rabbitmq/queues`).
2. Demostrar la existencia de las 3 colas funcionales y sus respectivas DLQ:
   - `pedidos.pedido-creado.queue` & `pedidos.pedido-creado.dlq`
   - `pedidos.notificacion.queue` & `pedidos.notificacion.dlq`
   - `pedidos.facturacion.queue` & `pedidos.facturacion.dlq`
3. Crear un pedido y mostrar en la consola de Spring Boot los logs de publicación y procesamiento de los 3 eventos en paralelo.
4. Demostrar el manejo de errores: enviar un mensaje corrupto y verificar en RabbitMQ que entra a su DLQ con cabeceras `x-exception-message` y `x-retry-count`.

### Minuto 5 a 7: API REST de Administración de RabbitMQ
1. Utilizar el archivo [backend/requests.http](file:///c:/Users/zente/OneDrive/Escritorio/Cloud/CloudNative/backend/requests.http) en el editor:
   - Ejecutar `GET /api/admin/rabbitmq/status` -> 200 OK con conteo de colas, exchanges y estado `CONNECTED`.
   - Ejecutar `POST /api/admin/rabbitmq/queues` -> Crear una cola temporal `demo.evaluacion.queue`.
   - Ejecutar `DELETE /api/admin/rabbitmq/queues/pedidos.pedido-creado.queue` -> **400 Bad Request** / Error controlado: se protege la cola crítica del sistema.
   - Ejecutar `DELETE /api/admin/rabbitmq/queues/demo.evaluacion.queue` -> 204 No Content: eliminación exitosa de cola no crítica.

### Minuto 7 a 10: Clúster RabbitMQ & Despliegue Cloud
1. Ejecutar en la terminal:
   ```bash
   docker exec -it pedidos360-rabbit1 rabbitmqctl cluster_status
   ```
2. Mostrar que ambos nodos `rabbit@rabbit1` y `rabbit@rabbit2` figuran en `Running Nodes`.
3. Explicar el archivo `.github/workflows/frontend-deploy.yml` (despliegue en GitHub Pages) y `backend-deploy.yml` (despliegue automático a EC2 con validación de secretos).
