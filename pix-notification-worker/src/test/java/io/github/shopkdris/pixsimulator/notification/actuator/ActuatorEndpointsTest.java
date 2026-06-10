package io.github.shopkdris.pixsimulator.notification.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do Actuator no {@code pix-notification-worker} (Lote 8).
 *
 * <p>O worker passou a subir uma camada web minima apenas para servir os
 * endpoints operacionais. Sobe um contexto web minimo (web + actuator), excluindo
 * MongoDB e Kafka para NAO depender de servicos externos. Valida que
 * {@code /actuator/health} responde e que {@code /actuator/env} NAO esta exposto.
 */
@SpringBootTest(
        classes = ActuatorEndpointsTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoints.web.exposure.include=health,info",
                "management.endpoint.health.show-details=never"
        })
class ActuatorEndpointsTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class,
            KafkaAutoConfiguration.class
    })
    static class TestApp {
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("/actuator/health responde 200 UP no worker")
    void healthIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("UP"),
                "o corpo do health deve indicar status UP");
    }

    @Test
    @DisplayName("/actuator/env NAO esta exposto no worker (404)")
    void envIsNotExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/env"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
