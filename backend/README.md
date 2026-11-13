# Backend

Consulta [la guía principal](../README.md). El perfil local usa H2 persistente, RabbitMQ y Kafka en este equipo, y valida tokens de acceso Microsoft. Inicio: mvn spring-boot:run. Pruebas: mvn test. La política de los tres administradores y sus vinculaciones oid está en security/AdminEmailPolicy.java.

## PostgreSQL en la nube

Configura estas variables en el servicio Spring Boot que ejecuta la API.

| Variable | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL. En RDS usa el certificado CA de la instancia y el modo TLS `verify-full`. |
| `SPRING_DATASOURCE_DRIVER` | `org.postgresql.Driver` |
| `SPRING_JPA_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña inyectada desde el entorno del servicio |

Usa las variables del entorno de despliegue para los brokers, Microsoft Entra ID, CORS y correo. Consulta [la guía de publicación](../deploy/PUBLICAR.md).
