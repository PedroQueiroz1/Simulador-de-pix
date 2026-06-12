package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox;

import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao do adapter JPA da Outbox usando SQL Server real via
 * Testcontainers.
 *
 * <p>Sobe o contexto completo: o Flyway cria o schema (inclusive a V4) e o
 * Hibernate valida ({@code ddl-auto=validate}). O publisher assincrono e
 * desligado ({@code pix.outbox.publisher.enabled=false}) para nao concorrer com
 * os eventos manipulados aqui. Cada teste comeca com a tabela limpa.
 */
@SpringBootTest
@Testcontainers
class JpaOutboxRepositoryAdapterIntegrationTest {

    @Container
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:sqlserver://" + SQL_SERVER.getHost() + ":" + SQL_SERVER.getFirstMappedPort()
                        + ";databaseName=master;encrypt=true;trustServerCertificate=true");
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        // Sem publisher concorrente durante os testes do adapter.
        registry.add("pix.outbox.publisher.enabled", () -> false);
    }

    private static final String TOPIC = "pix.payment.events";
    private static final String PAYLOAD = "{\"eventType\":\"PAYMENT_CREATED\"}";

    @Autowired
    private JpaOutboxRepositoryAdapter adapter;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM outbox_events");
    }

    private OutboxEvent pendingEvent(UUID aggregateId, String eventType, LocalDateTime availableAt) {
        UUID id = idGenerator.generate();
        return OutboxEvent.restore(
                id, "PAYMENT", aggregateId, eventType, 1,
                TOPIC, aggregateId.toString(), PAYLOAD,
                OutboxEventStatus.PENDING, 0, null,
                LocalDateTime.now(), null, availableAt);
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_events WHERE id = ?", String.class, id.toString());
    }

    @Test
    @DisplayName("Flyway (V4) deve ter criado a tabela outbox_events")
    void migrationV4ShouldHaveCreatedOutboxTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'outbox_events'",
                Integer.class);

        assertEquals(1, count);
    }

    @Test
    @DisplayName("Deve salvar evento na tabela outbox_events")
    void shouldSaveEvent() {
        OutboxEvent event = pendingEvent(idGenerator.generate(), "PAYMENT_CREATED", LocalDateTime.now());

        OutboxEvent saved = adapter.save(event);

        assertEquals(event.getId(), saved.getId());
        assertEquals(OutboxEventStatus.PENDING.name(), statusOf(saved.getId()));
    }

    @Test
    @DisplayName("Deve buscar eventos PENDING por availableAt (ignorando os ainda indisponiveis)")
    void shouldFindPendingEventsByAvailableAt() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent available = pendingEvent(idGenerator.generate(), "PAYMENT_CREATED", now.minusMinutes(1));
        OutboxEvent future = pendingEvent(idGenerator.generate(), "PAYMENT_CREATED", now.plusHours(1));
        adapter.save(available);
        adapter.save(future);

        List<OutboxEvent> pending = adapter.findPendingEvents(10, now);

        assertTrue(pending.stream().anyMatch(e -> e.getId().equals(available.getId())));
        assertTrue(pending.stream().noneMatch(e -> e.getId().equals(future.getId())));
    }

    @Test
    @DisplayName("Deve marcar evento como PUBLISHED")
    void shouldMarkAsPublished() {
        OutboxEvent event = pendingEvent(idGenerator.generate(), "PAYMENT_CREATED", LocalDateTime.now());
        adapter.save(event);

        adapter.markAsPublished(event.getId(), LocalDateTime.now());

        assertEquals(OutboxEventStatus.PUBLISHED.name(), statusOf(event.getId()));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT published_at FROM outbox_events WHERE id = ?",
                java.sql.Timestamp.class, event.getId().toString()));
    }

    @Test
    @DisplayName("Deve marcar evento como FAILED ao esgotar tentativas")
    void shouldMarkAsFailed() {
        OutboxEvent event = pendingEvent(idGenerator.generate(), "PAYMENT_CREATED", LocalDateTime.now());
        adapter.save(event);

        // max-attempts = 1: uma falha ja leva a FAILED.
        event.registerFailedAttempt("kafka down", LocalDateTime.now().plusSeconds(5), 1);
        adapter.markAsFailed(event);

        assertEquals(OutboxEventStatus.FAILED.name(), statusOf(event.getId()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT attempts FROM outbox_events WHERE id = ?", Integer.class, event.getId().toString()));
        assertEquals("kafka down", jdbcTemplate.queryForObject(
                "SELECT last_error FROM outbox_events WHERE id = ?", String.class, event.getId().toString()));
    }

    @Test
    @DisplayName("Deve impedir duplicidade de aggregateId + eventType (unique index)")
    void shouldRejectDuplicateAggregateAndEventType() {
        UUID aggregateId = idGenerator.generate();
        adapter.save(pendingEvent(aggregateId, "PAYMENT_CREATED", LocalDateTime.now()));

        // Mesmo aggregateId + eventType, id diferente: a unique deve barrar.
        OutboxEvent duplicate = pendingEvent(aggregateId, "PAYMENT_CREATED", LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class, () -> adapter.save(duplicate));
    }
}
