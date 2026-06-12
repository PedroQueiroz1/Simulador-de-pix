package io.pixsimulator.payment.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do Actuator no {@code pix-payment-api}.
 *
 * <p>Sobe um contexto web minimo (apenas web + actuator), excluindo as
 * auto-configuracoes de infraestrutura (SQL Server/JPA/Flyway, Redis, Kafka) para
 * NAO depender de servicos externos. Valida que {@code /actuator/health} responde
 * e que endpoints sensiveis como {@code /actuator/env} NAO estao expostos.
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
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
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
    @DisplayName("/actuator/health responde 200 UP")
    void healthIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("UP"),
                "o corpo do health deve indicar status UP");
    }

    @Test
    @DisplayName("/actuator/env NAO esta exposto (404)")
    void envIsNotExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/env"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("/actuator/beans NAO esta exposto (404)")
    void beansIsNotExposed() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/beans"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
