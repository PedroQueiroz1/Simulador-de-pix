package io.pixsimulator.notification.adapter.out.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import io.pixsimulator.notification.application.exception.DuplicateEventException;
import io.pixsimulator.notification.domain.NotificationAudit;
import io.pixsimulator.notification.domain.NotificationStatus;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao do {@link MongoNotificationAuditRepositoryAdapter} usando
 * MongoDB real via Testcontainers (Lote 7).
 *
 * <p>Nao sobe o contexto Spring completo (que tambem exigiria Kafka): monta
 * manualmente o {@link MongoTemplate} apontado para o container, cria o indice
 * unico de {@code eventId} (que no runtime e criado por
 * {@code auto-index-creation}) e instancia o repositorio Spring Data via
 * {@link MongoRepositoryFactory}. Assim valida persistencia, busca por eventId e
 * a garantia de unicidade contra um MongoDB de verdade, isolado.
 */
@Testcontainers
class MongoNotificationAuditRepositoryAdapterIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    private static MongoTemplate mongoTemplate;
    private static MongoNotificationAuditRepositoryAdapter adapter;

    @BeforeAll
    static void setUp() {
        // O campo eventId e UUID: o driver exige uuidRepresentation explicito para
        // codifica-lo. Em runtime o Spring Boot ja configura isso; aqui, como
        // montamos o client manualmente, definimos STANDARD (consistente leitura/escrita).
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO.getConnectionString()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        mongoTemplate = new MongoTemplate(MongoClients.create(settings), "pix_notifications_test");
        // Indice unico de eventId: e o que garante a idempotencia de consumo.
        mongoTemplate.indexOps(NotificationAuditDocument.class)
                .ensureIndex(new Index().on("eventId", Sort.Direction.ASC).unique());

        SpringDataNotificationAuditRepository repository =
                new MongoRepositoryFactory(mongoTemplate).getRepository(SpringDataNotificationAuditRepository.class);
        adapter = new MongoNotificationAuditRepositoryAdapter(repository);
    }

    private static final String CORRELATION_ID = "11111111-2222-3333-4444-555555555555";

    private NotificationAudit processedAudit(UUID eventId) {
        return NotificationAudit.processed(
                eventId, "PAYMENT_CREATED", 1, UUID.randomUUID(), CORRELATION_ID,
                "Notificacao simulada", "{\"eventId\":\"" + eventId + "\"}",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private NotificationAudit failedAudit(UUID eventId) {
        return NotificationAudit.failed(
                eventId, null, null, null, CORRELATION_ID, "Erro controlado de teste",
                "{\"eventId\":\"" + eventId + "\"}", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve salvar uma auditoria e gerar o id do MongoDB")
    void savesAudit() {
        UUID eventId = UUID.randomUUID();

        NotificationAudit saved = adapter.save(processedAudit(eventId));

        assertNotNull(saved.getId(), "o MongoDB deve gerar o id");
        assertEquals(eventId, saved.getEventId());
    }

    @Test
    @DisplayName("Deve buscar uma auditoria por eventId")
    void findsByEventId() {
        UUID eventId = UUID.randomUUID();
        adapter.save(processedAudit(eventId));

        Optional<NotificationAudit> found = adapter.findByEventId(eventId);

        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getEventId());
        assertEquals("PAYMENT_CREATED", found.get().getEventType());
    }

    @Test
    @DisplayName("Lote 8: deve persistir e recuperar o correlationId da auditoria")
    void persistsCorrelationId() {
        UUID eventId = UUID.randomUUID();
        adapter.save(processedAudit(eventId));

        Optional<NotificationAudit> found = adapter.findByEventId(eventId);

        assertTrue(found.isPresent());
        assertEquals(CORRELATION_ID, found.get().getCorrelationId());
    }

    @Test
    @DisplayName("Deve refletir existsByEventId apos salvar e retornar falso para eventId inexistente")
    void verifiesExistsByEventId() {
        UUID eventId = UUID.randomUUID();
        assertFalse(adapter.existsByEventId(eventId));

        adapter.save(processedAudit(eventId));

        assertTrue(adapter.existsByEventId(eventId));
        assertFalse(adapter.existsByEventId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Deve impedir duplicidade de eventId (indice unico) lancando DuplicateEventException")
    void preventsDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        adapter.save(processedAudit(eventId));

        assertThrows(DuplicateEventException.class, () -> adapter.save(processedAudit(eventId)));
    }

    @Test
    @DisplayName("Deve salvar e recuperar auditoria com status PROCESSED")
    void savesProcessedStatus() {
        UUID eventId = UUID.randomUUID();
        adapter.save(processedAudit(eventId));

        Optional<NotificationAudit> found = adapter.findByEventId(eventId);
        assertTrue(found.isPresent());
        assertEquals(NotificationStatus.PROCESSED, found.get().getNotificationStatus());
        assertNotNull(found.get().getNotificationMessage());
    }

    @Test
    @DisplayName("Deve salvar e recuperar auditoria com status FAILED")
    void savesFailedStatus() {
        UUID eventId = UUID.randomUUID();
        adapter.save(failedAudit(eventId));

        Optional<NotificationAudit> found = adapter.findByEventId(eventId);
        assertTrue(found.isPresent());
        assertEquals(NotificationStatus.FAILED, found.get().getNotificationStatus());
        assertEquals("Erro controlado de teste", found.get().getErrorMessage());
    }
}
