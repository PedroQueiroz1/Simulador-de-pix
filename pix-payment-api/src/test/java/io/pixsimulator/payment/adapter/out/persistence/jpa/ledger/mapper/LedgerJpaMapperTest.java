package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerEntryEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerTransactionEntity;
import io.pixsimulator.payment.domain.ledger.LedgerEntry;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes do mapper manual entre dominio do Ledger e entidades JPA (Lote 5).
 */
class LedgerJpaMapperTest {

    private static final UUID TX_ID = UUID.fromString("0197a0e9-67b7-7a2d-b870-f0ef4f11662f");
    private static final UUID DEBIT_ID = UUID.fromString("0197a0e9-6a20-7fd6-903e-b12560db94a1");
    private static final UUID CREDIT_ID = UUID.fromString("0197a0e9-6bf9-7bec-ae34-7bd01e68f1c4");
    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

    private LedgerTransaction domainSettlement() {
        return LedgerTransaction.createPixSettlement(
                TX_ID, DEBIT_ID, CREDIT_ID, PAYMENT_ID, PAYER, RECEIVER, AMOUNT, AT);
    }

    private LedgerTransactionEntity entitySettlement() {
        LedgerEntryEntity debit = new LedgerEntryEntity(
                DEBIT_ID, TX_ID, PAYMENT_ID, PAYER, "DEBIT", AMOUNT, AT);
        LedgerEntryEntity credit = new LedgerEntryEntity(
                CREDIT_ID, TX_ID, PAYMENT_ID, RECEIVER, "CREDIT", AMOUNT, AT);
        return new LedgerTransactionEntity(
                TX_ID, PAYMENT_ID, "PIX_SETTLEMENT", AT, List.of(debit, credit));
    }

    @Test
    @DisplayName("Deve converter LedgerTransaction (dominio) para Entity")
    void shouldMapDomainToEntity() {
        LedgerTransactionEntity entity = LedgerJpaMapper.toEntity(domainSettlement());

        assertEquals(TX_ID, entity.getId());
        assertEquals(PAYMENT_ID, entity.getPaymentId());
        assertEquals("PIX_SETTLEMENT", entity.getOperationType());
        assertEquals(AT, entity.getCreatedAt());
        assertEquals(2, entity.getEntries().size());
    }

    @Test
    @DisplayName("Deve converter Entity para LedgerTransaction (dominio)")
    void shouldMapEntityToDomain() {
        LedgerTransaction domain = LedgerJpaMapper.toDomain(entitySettlement());

        assertEquals(TX_ID, domain.getId());
        assertEquals(PAYMENT_ID, domain.getPaymentId());
        assertEquals(LedgerOperationType.PIX_SETTLEMENT, domain.getOperationType());
        assertEquals(AT, domain.getCreatedAt());
        assertEquals(2, domain.getEntries().size());
    }

    @Test
    @DisplayName("Deve mapear entries preservando direction e amount (dominio -> entity)")
    void shouldMapEntriesDomainToEntity() {
        LedgerTransactionEntity entity = LedgerJpaMapper.toEntity(domainSettlement());

        LedgerEntryEntity debit = entity.getEntries().stream()
                .filter(e -> e.getDirection().equals("DEBIT")).findFirst().orElseThrow();
        LedgerEntryEntity credit = entity.getEntries().stream()
                .filter(e -> e.getDirection().equals("CREDIT")).findFirst().orElseThrow();

        assertEquals(DEBIT_ID, debit.getId());
        assertEquals(PAYER, debit.getAccountKey());
        assertEquals(AMOUNT, debit.getAmount());
        assertEquals(PAYMENT_ID, debit.getPaymentId());
        assertEquals(TX_ID, debit.getLedgerTransactionId());

        assertEquals(CREDIT_ID, credit.getId());
        assertEquals(RECEIVER, credit.getAccountKey());
        assertEquals(AMOUNT, credit.getAmount());
    }

    @Test
    @DisplayName("Deve mapear entries preservando direction e amount (entity -> dominio)")
    void shouldMapEntriesEntityToDomain() {
        LedgerTransaction domain = LedgerJpaMapper.toDomain(entitySettlement());

        LedgerEntry debit = domain.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = domain.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.CREDIT).findFirst().orElseThrow();

        assertEquals(DEBIT_ID, debit.getId());
        assertEquals(PAYER, debit.getAccountKey());
        assertEquals(AMOUNT, debit.getAmount());
        assertEquals(LedgerEntryDirection.CREDIT, credit.getDirection());
        assertEquals(RECEIVER, credit.getAccountKey());
    }

    @Test
    @DisplayName("Round-trip dominio -> entity -> dominio deve preservar IDs, paymentId, operationType, direction e amount")
    void shouldRoundTripWithoutLoss() {
        LedgerTransaction original = domainSettlement();

        LedgerTransaction roundTrip = LedgerJpaMapper.toDomain(LedgerJpaMapper.toEntity(original));

        assertEquals(original.getId(), roundTrip.getId());
        assertEquals(original.getPaymentId(), roundTrip.getPaymentId());
        assertEquals(original.getOperationType(), roundTrip.getOperationType());
        assertEquals(original.getCreatedAt(), roundTrip.getCreatedAt());
        assertEquals(original.getEntries().size(), roundTrip.getEntries().size());

        LedgerEntry originalDebit = original.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT).findFirst().orElseThrow();
        LedgerEntry roundTripDebit = roundTrip.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT).findFirst().orElseThrow();

        assertEquals(originalDebit.getId(), roundTripDebit.getId());
        assertEquals(originalDebit.getAccountKey(), roundTripDebit.getAccountKey());
        assertEquals(originalDebit.getAmount(), roundTripDebit.getAmount());
        assertEquals(originalDebit.getPaymentId(), roundTripDebit.getPaymentId());
    }
}
