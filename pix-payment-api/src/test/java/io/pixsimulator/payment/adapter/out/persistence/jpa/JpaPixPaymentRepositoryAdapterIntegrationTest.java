package io.pixsimulator.payment.adapter.out.persistence.jpa;

import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao do adapter JPA usando SQL Server real via Testcontainers.
 *
 * <p>Sobe o contexto Spring completo: o Flyway cria o schema no container e o
 * Hibernate valida ({@code ddl-auto=validate}) a entity contra a tabela. Em
 * seguida exercita o adapter contra o banco de verdade (driver, tipos de
 * coluna, constraint unica), nao um H2 simulado.
 */
@SpringBootTest
@Testcontainers
class JpaPixPaymentRepositoryAdapterIntegrationTest {

    @Container
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        // Constroi a URL manualmente para garantir encrypt/trustServerCertificate
        // (mssql-jdbc 12+ exige isso). O database padrao do container e "master".
        registry.add("spring.datasource.url", () ->
                "jdbc:sqlserver://" + SQL_SERVER.getHost() + ":" + SQL_SERVER.getFirstMappedPort()
                        + ";databaseName=master;encrypt=true;trustServerCertificate=true");
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
    }

    @Autowired
    private JpaPixPaymentRepositoryAdapter adapter;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PixPayment newPayment(String idempotencyKey) {
        return PixPayment.create(
                idGenerator.generate(),
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                idempotencyKey);
    }

    @Test
    @DisplayName("Flyway deve ter criado a tabela pix_payments")
    void flywayShouldHaveCreatedTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'pix_payments'",
                Integer.class);

        assertEquals(1, count);
    }

    @Test
    @DisplayName("Deve salvar pagamento no SQL Server e retornar o dominio")
    void shouldSaveAndReturnDomain() {
        PixPayment payment = newPayment("key-save-" + UUID.randomUUID());

        PixPayment saved = adapter.save(payment);

        assertEquals(payment.getId(), saved.getId());
        assertEquals(PixPaymentStatus.CREATED, saved.getStatus());
        assertEquals(payment.getIdempotencyKey(), saved.getIdempotencyKey());
        assertEquals(new BigDecimal("150.75"), saved.getAmount());
    }

    @Test
    @DisplayName("Deve preservar o paymentId UUIDv7 apos persistir")
    void shouldPreserveUuidV7Id() {
        PixPayment payment = newPayment("key-uuid-" + UUID.randomUUID());

        PixPayment saved = adapter.save(payment);

        assertEquals(payment.getId(), saved.getId());
        assertEquals(7, saved.getId().version(), "o id deve continuar sendo UUID versao 7");
    }

    @Test
    @DisplayName("Deve buscar pagamento por idempotencyKey")
    void shouldFindByIdempotencyKey() {
        String key = "key-find-" + UUID.randomUUID();
        PixPayment payment = newPayment(key);
        adapter.save(payment);

        Optional<PixPayment> found = adapter.findByIdempotencyKey(key);

        assertTrue(found.isPresent());
        assertEquals(payment.getId(), found.get().getId());
        assertEquals(key, found.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("Deve retornar vazio quando idempotencyKey nao existir")
    void shouldReturnEmptyWhenKeyDoesNotExist() {
        Optional<PixPayment> found =
                adapter.findByIdempotencyKey("key-inexistente-" + UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("O banco deve impedir duplicidade de idempotencyKey (constraint unica)")
    void shouldRejectDuplicateIdempotencyKey() {
        String key = "key-dup-" + UUID.randomUUID();
        adapter.save(newPayment(key));

        // Mesmo idempotencyKey, id diferente: a constraint UK deve barrar.
        PixPayment duplicate = newPayment(key);

        assertThrows(DataIntegrityViolationException.class, () -> adapter.save(duplicate));
    }
}
