# =========================================================================
# DOCKERFILE - BACKEND SPRING BOOT (PEDIDOS360)
# Multi-stage build optimizado para Java 21
# =========================================================================

# Etapa 1: Compilacion y empaquetado con Maven
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace/backend

# Copiar definicion de dependencias y fuentes
COPY pom.xml .
COPY src ./src

# Empaquetar artefacto ejecutable omitiendo pruebas en el empaquetado de imagen
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecucion con JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copiar el archivo JAR generado desde la etapa previa
COPY --from=builder /workspace/backend/target/pedidos360-backend-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto HTTP del microservicio
EXPOSE 8080

# Variables de entorno predeterminadas para conexion en red Docker
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8080 \
    SPRING_RABBITMQ_HOST=rabbit1 \
    SPRING_RABBITMQ_PORT=5672

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/public/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
