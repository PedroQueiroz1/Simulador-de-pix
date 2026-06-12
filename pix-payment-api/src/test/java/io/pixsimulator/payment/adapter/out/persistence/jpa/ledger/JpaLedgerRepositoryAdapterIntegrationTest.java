package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger;

import io.pixsimulator.payment.adapter.out.persistence.jpa.JpaPixPaymentRepositoryAdapter;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao do adapter JPA do Ledger usando SQL Server real via
 * Testcontainers.
 *
 * <p>Sobe o contexto completo: o Flyway cria o schema (inclusive a V3) e o
 * Hibernate valida ({@code ddl-auto=validate}). Como {@code ledger_*} tem FK
 * para {@code pix_payments}, cada cenario primeiro persiste um pagamento.
 */
@SpringBootTest
@Testcontainers
class JpaLedgerRepositoryAdapterIntegrationTest {

    @Container
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:sqlserver://" + SQL_SERVER.getHost() + ":" + SQL_SERVER.getFirstMappedPort()
                        + ";databaseName=master;encrypt=true;trustServerCertificate=true");
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        // O publisher assincrono nao deve rodar neste teste (evita ruido
        // e bloqueio ao buscar conexao durante o shutdown do contexto).
        registry.add("pix.outbox.publisher.enabled", () -> false);
    }

    @Autowired
    private JpaLedgerRepositoryAdapter ledgerAdapter;

    @Autowired
    private JpaPixPaymentRepositoryAdapter paymentAdapter;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final BigDecimal AMOUNT = new BigDecimal("150.75");

    /** Cria e persiste um pagamento (satisfaz a FK do ledger) e o devolve. */
    private PixPayment savedPayment() {
        PixPayment payment = PixPayment.create(
                idGenerator.generate(),
                "11111111111",
                "22222222222",
                AMOUNT,
                "Pagamento de teste",
                "key-ledger-" + UUID.randomUUID());
        return paymentAdapter.save(payment);
    }

    private LedgerTransaction settlementFor(PixPayment payment) {
        return LedgerTransaction.createPixSettlement(
                idGenerator.generate(),
                idGenerator.generate(),
                idGenerator.generate(),
                payment.getId(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                LocalDateTime.now());
    }

    @Test
    @DisplayName("Flyway (V3) deve ter criado ledger_transactions e ledger_entries")
    void migrationV3ShouldHaveCreatedLedgerTables() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_NAME IN ('ledger_transactions', 'ledger_entries')",
                Integer.class);

        assertEquals(2, count);
    }

    @Test
    @DisplayName("Deve salvar LedgerTransaction com entries e garantir persistencia")
    void shouldSaveLedgerTransactionWithEntries() {
        PixPayment payment = savedPayment();

        LedgerTransaction saved = ledgerAdapter.save(settlementFor(payment));

        assertEquals(payment.getId(), saved.getPaymentId());
        assertEquals(2, saved.getEntries().size());

        // Round-trip pelo banco: as entries foram realmente persistidas.
        LedgerTransaction reloaded = ledgerAdapter
                .findByPaymentIdAndOperationType(payment.getId(), LedgerOperationType.PIX_SETTLEMENT)
                .orElseThrow();
        assertEquals(2, reloaded.getEntries().size());
        assertTrue(reloaded.getEntries().stream().anyMatch(e -> e.getDirection() == LedgerEntryDirection.DEBIT));
        assertTrue(reloaded.getEntries().stream().anyMatch(e -> e.getDirection() == LedgerEntryDirection.CREDIT));
    }

    @Test
    @DisplayName("Deve buscar ledger por paymentId")
    void shouldFindByPaymentId() {
        PixPayment payment = savedPayment();
        LedgerTransaction saved = ledgerAdapter.save(settlementFor(payment));

        List<LedgerTransaction> found = ledgerAdapter.findByPaymentId(payment.getId());

        assertEquals(1, found.size());
        assertEquals(saved.getId(), found.get(0).getId());
        assertEquals(2, found.get(0).getEntries().size());
    }

    @Test
    @DisplayName("Deve buscar ledger por paymentId + operationType")
    void shouldFindByPaymentIdAndOperationType() {
        PixPayment payment = savedPayment();
        LedgerTransaction saved = ledgerAdapter.save(settlementFor(payment));

        Optional<LedgerTransaction> found = ledgerAdapter
                .findByPaymentIdAndOperationType(payment.getId(), LedgerOperationType.PIX_SETTLEMENT);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o pagamento nao tiver ledger")
    void shouldReturnEmptyWhenNoLedger() {
        PixPayment payment = savedPayment();

        assertTrue(ledgerAdapter.findByPaymentId(payment.getId()).isEmpty());
        assertTrue(ledgerAdapter
                .findByPaymentIdAndOperationType(payment.getId(), LedgerOperationType.PIX_SETTLEMENT)
                .isEmpty());
    }

    @Test
    @DisplayName("O banco deve impedir duplicidade de paymentId + operationType (constraint unica)")
    void shouldRejectDuplicatePaymentOperation() {
        PixPayment payment = savedPayment();
        ledgerAdapter.save(settlementFor(payment));

        // Mesmo paymentId + PIX_SETTLEMENT, ids diferentes: a UK deve barrar.
        LedgerTransaction duplicate = settlementFor(payment);

        assertThrows(DataIntegrityViolationException.class, () -> ledgerAdapter.save(duplicate));
    }
}
