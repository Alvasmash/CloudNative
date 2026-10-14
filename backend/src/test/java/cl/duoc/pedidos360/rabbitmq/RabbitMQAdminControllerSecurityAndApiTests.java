package cl.duoc.pedidos360.rabbitmq;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pedidos360.config.RabbitMQConstants;

/**
 * =========================================================================
 * PRUEBAS DE INTEGRACION: RabbitMQAdminController (Seguridad y API REST)
 * =========================================================================
 * Criterio de Evaluacion EP3 (13%): Endpoints REST para administrar RabbitMQ.
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros y respuestas semanticas.
 * Criterio de Evaluacion EP3 (8.0): Seguridad RBAC (401 sin token, 403 sin ROLE_ADMIN, 200/201 con ROLE_ADMIN).
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(RabbitMQAdminControllerSecurityAndApiTests.TestSecurityConfig.class)
class RabbitMQAdminControllerSecurityAndApiTests {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> new Jwt(
                    token,
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    Map.of(
                            "sub", "admin-user",
                            "scp", "recurso.read recurso.write",
                            "aud", List.of("api://pedidos360-api")
                    )
            );
        }

        @Bean
        @Primary
        org.springframework.amqp.rabbit.core.RabbitTemplate testRabbitTemplate() {
            return org.mockito.Mockito.mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
        }

        @Bean
        @Primary
        org.springframework.amqp.rabbit.core.RabbitAdmin testRabbitAdmin() {
            return org.mockito.Mockito.mock(org.springframework.amqp.rabbit.core.RabbitAdmin.class);
        }
    }

    @Test
    @DisplayName("1. GET /api/admin/rabbitmq/status sin token responde 401 Unauthorized")
    void getStatus_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/rabbitmq/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("2. GET /api/admin/rabbitmq/status con usuario sin rol ADMIN responde 403 Forbidden")
    void getStatus_withoutAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/rabbitmq/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("3. GET /api/admin/rabbitmq/status con rol ROLE_ADMIN responde 200 OK")
    void getStatus_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/admin/rabbitmq/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.host").isNotEmpty())
                .andExpect(jsonPath("$.totalQueues").isNumber());
    }

    @Test
    @DisplayName("4. GET /api/admin/rabbitmq/queues con rol ROLE_ADMIN lista las colas declaradas")
    void listQueues_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/admin/rabbitmq/queues")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("5. POST /api/admin/rabbitmq/queues con datos validos responde 201 Created")
    void createQueue_validRequest_shouldReturn201() throws Exception {
        String payload = """
                {
                    "name": "cola.test.integracion",
                    "durable": true,
                    "exclusive": false,
                    "autoDelete": false
                }
                """;

        mockMvc.perform(post("/api/admin/rabbitmq/queues")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("cola.test.integracion"))
                .andExpect(jsonPath("$.durable").value(true));
    }

    @Test
    @DisplayName("6. POST /api/admin/rabbitmq/queues con nombre con caracteres invalidos responde 400 Bad Request")
    void createQueue_invalidName_shouldReturn400() throws Exception {
        String payload = """
                {
                    "name": "cola con espacios y simbolos *#%",
                    "durable": true
                }
                """;

        mockMvc.perform(post("/api/admin/rabbitmq/queues")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").isNotEmpty());
    }

    @Test
    @DisplayName("7. DELETE /api/admin/rabbitmq/queues/{name} sobre cola critica del sistema responde 409 Conflict")
    void deleteQueue_criticalQueue_shouldReturn409() throws Exception {
        mockMvc.perform(delete("/api/admin/rabbitmq/queues/" + RabbitMQConstants.QUEUE_PEDIDO_CREADO)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("8. POST /api/admin/rabbitmq/exchanges con tipo no admitido responde 400 Bad Request")
    void createExchange_invalidType_shouldReturn400() throws Exception {
        String payload = """
                {
                    "name": "exchange.invalido",
                    "type": "tipo_desconocido"
                }
                """;

        mockMvc.perform(post("/api/admin/rabbitmq/exchanges")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("9. POST /api/admin/rabbitmq/exchanges con tipo valido responde 201 Created")
    void createExchange_validRequest_shouldReturn201() throws Exception {
        String payload = """
                {
                    "name": "pedidos.notificaciones.exchange",
                    "type": "topic",
                    "durable": true
                }
                """;

        mockMvc.perform(post("/api/admin/rabbitmq/exchanges")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pedidos.notificaciones.exchange"))
                .andExpect(jsonPath("$.type").value("topic"));
    }

    @Test
    @DisplayName("10. DELETE /api/admin/rabbitmq/exchanges/{name} sobre exchange critico responde 409 Conflict")
    void deleteExchange_criticalExchange_shouldReturn409() throws Exception {
        mockMvc.perform(delete("/api/admin/rabbitmq/exchanges/" + RabbitMQConstants.EXCHANGE_PEDIDOS)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("11. POST /api/admin/rabbitmq/bindings con recurso inexistente responde 404 Not Found")
    void createBinding_nonExistentResource_shouldReturn404() throws Exception {
        String payload = """
                {
                    "queueName": "cola.inexistente.999",
                    "exchangeName": "%s",
                    "routingKey": "test.rk"
                }
                """.formatted(RabbitMQConstants.EXCHANGE_PEDIDOS);

        mockMvc.perform(post("/api/admin/rabbitmq/bindings")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("12. POST /api/admin/rabbitmq/bindings con recursos validos responde 201 Created")
    void createBinding_validResources_shouldReturn201() throws Exception {
        String payload = """
                {
                    "queueName": "%s",
                    "exchangeName": "%s",
                    "routingKey": "pedido.especial.test"
                }
                """.formatted(RabbitMQConstants.QUEUE_PEDIDO_CREADO, RabbitMQConstants.EXCHANGE_PEDIDOS);

        mockMvc.perform(post("/api/admin/rabbitmq/bindings")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueName").value(RabbitMQConstants.QUEUE_PEDIDO_CREADO))
                .andExpect(jsonPath("$.routingKey").value("pedido.especial.test"));
    }
}
