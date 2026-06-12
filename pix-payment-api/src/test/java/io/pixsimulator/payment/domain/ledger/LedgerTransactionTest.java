package io.pixsimulator.payment.domain.ledger;

import io.pixsimulator.payment.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do dominio do Ledger: regras de dupla entrada, fechamento em zero,
 * valor positivo e imutabilidade (append-only).
 */
class LedgerTransactionTest {

    private static final UUID LEDGER_TX_ID = UUID.fromString("0197a0e9-67b7-7a2d-b870-f0ef4f11662f");
    private static final UUID DEBIT_ID = UUID.fromString("0197a0e9-6a20-7fd6-903e-b12560db94a1");
    private static final UUID CREDIT_ID = UUID.fromString("0197a0e9-6bf9-7bec-ae34-7bd01e68f1c4");
    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

    private LedgerTransaction validSettlement() {
        return LedgerTransaction.createPixSettlement(
                LEDGER_TX_ID, DEBIT_ID, CREDIT_ID, PAYMENT_ID, PAYER, RECEIVER, AMOUNT, NOW);
    }

    private LedgerEntry debit(BigDecimal amount) {
        return LedgerEntry.create(DEBIT_ID, LEDGER_TX_ID, PAYMENT_ID, PAYER,
                LedgerEntryDirection.DEBIT, amount, NOW);
    }

    private LedgerEntry credit(BigDecimal amount) {
        return LedgerEntry.create(CREDIT_ID, LEDGER_TX_ID, PAYMENT_ID, RECEIVER,
                LedgerEntryDirection.CREDIT, amount, NOW);
    }

    @Test
    @DisplayName("Deve criar ledger PIX_SETTLEMENT valido com 1 DEBIT e 1 CREDIT")
    void shouldCreateValidPixSettlement() {
        LedgerTransaction tx = validSettlement();

        assertEquals(LedgerOperationType.PIX_SETTLEMENT, tx.getOperationType());
        assertEquals(2, tx.getEntries().size());

        LedgerEntry debit = tx.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = tx.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.CREDIT).findFirst().orElseThrow();

        assertEquals(PAYER, debit.getAccountKey());
        assertEquals(AMOUNT, debit.getAmount());
        assertEquals(RECEIVER, credit.getAccountKey());
        assertEquals(AMOUNT, credit.getAmount());
    }

    @Test
    @DisplayName("Deve rejeitar ledger sem DEBIT")
    void shouldRejectLedgerWithoutDebit() {
        List<LedgerEntry> onlyCredit = List.of(credit(AMOUNT));

        DomainException ex = assertThrows(DomainException.class, () ->
                LedgerTransaction.restore(LEDGER_TX_ID, PAYMENT_ID,
                        LedgerOperationType.PIX_SETTLEMENT, onlyCredit, NOW));

        assertTrue(ex.getMessage().contains("DEBIT"));
    }

    @Test
    @DisplayName("Deve rejeitar ledger sem CREDIT")
    void shouldRejectLedgerWithoutCredit() {
        List<LedgerEntry> onlyDebit = List.of(debit(AMOUNT));

        DomainException ex = assertThrows(DomainException.class, () ->
                LedgerTransaction.restore(LEDGER_TX_ID, PAYMENT_ID,
                        LedgerOperationType.PIX_SETTLEMENT, onlyDebit, NOW));

        assertTrue(ex.getMessage().contains("CREDIT"));
    }

    @Test
    @DisplayName("Deve rejeitar ledger com total de debitos diferente do total de creditos")
    void shouldRejectUnbalancedLedger() {
        List<LedgerEntry> unbalanced = List.of(debit(new BigDecimal("100.00")), credit(new BigDecimal("50.00")));

        DomainException ex = assertThrows(DomainException.class, () ->
                LedgerTransaction.restore(LEDGER_TX_ID, PAYMENT_ID,
                        LedgerOperationType.PIX_SETTLEMENT, unbalanced, NOW));

        assertTrue(ex.getMessage().contains("balance"));
    }

    @Test
    @DisplayName("Deve rejeitar entry com amount zero")
    void shouldRejectZeroAmount() {
        assertThrows(DomainException.class, () ->
                LedgerTransaction.createPixSettlement(
                        LEDGER_TX_ID, DEBIT_ID, CREDIT_ID, PAYMENT_ID, PAYER, RECEIVER,
                        new BigDecimal("0.00"), NOW));
    }

    @Test
    @DisplayName("Deve rejeitar entry com amount negativo")
    void shouldRejectNegativeAmount() {
        assertThrows(DomainException.class, () ->
                LedgerTransaction.createPixSettlement(
                        LEDGER_TX_ID, DEBIT_ID, CREDIT_ID, PAYMENT_ID, PAYER, RECEIVER,
                        new BigDecimal("-10.00"), NOW));
    }

    @Test
    @DisplayName("Deve preservar paymentId")
    void shouldPreservePaymentId() {
        assertEquals(PAYMENT_ID, validSettlement().getPaymentId());
        validSettlement().getEntries().forEach(e -> assertEquals(PAYMENT_ID, e.getPaymentId()));
    }

    @Test
    @DisplayName("Deve preservar operationType")
    void shouldPreserveOperationType() {
        assertEquals(LedgerOperationType.PIX_SETTLEMENT, validSettlement().getOperationType());
    }

    @Test
    @DisplayName("Deve ser append-only: a lista de entries e imutavel")
    void shouldBeAppendOnly() {
        LedgerTransaction tx = validSettlement();
        LedgerEntry extra = debit(AMOUNT);

        assertThrows(UnsupportedOperationException.class, () -> tx.getEntries().add(extra));
        assertThrows(UnsupportedOperationException.class, () -> tx.getEntries().clear());
    }

    @Test
    @DisplayName("A transacao deve preservar o proprio id")
    void shouldPreserveTransactionId() {
        LedgerTransaction tx = validSettlement();
        assertSame(LedgerOperationType.PIX_SETTLEMENT, tx.getOperationType());
        assertEquals(LEDGER_TX_ID, tx.getId());
    }
}
