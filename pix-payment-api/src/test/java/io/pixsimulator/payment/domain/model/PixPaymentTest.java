package io.pixsimulator.payment.domain.model;

import io.pixsimulator.payment.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PixPaymentTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final String DESCRIPTION = "Pagamento de teste";
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    @Test
    @DisplayName("Deve criar pagamento valido com status CREATED")
    void shouldCreateValidPaymentWithCreatedStatus() {
        PixPayment payment = PixPayment.create(ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY);

        assertNotNull(payment);
        assertEquals(PixPaymentStatus.CREATED, payment.getStatus());
        assertEquals(ID, payment.getId());
        assertEquals(PAYER, payment.getPayerKey());
        assertEquals(RECEIVER, payment.getReceiverKey());
        assertEquals(AMOUNT, payment.getAmount());
        assertEquals(DESCRIPTION, payment.getDescription());
    }

    @Test
    @DisplayName("Deve preencher createdAt na criacao")
    void shouldFillCreatedAt() {
        PixPayment payment = PixPayment.create(ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY);

        assertNotNull(payment.getCreatedAt());
    }

    @Test
    @DisplayName("Deve preservar a idempotencyKey")
    void shouldPreserveIdempotencyKey() {
        PixPayment payment = PixPayment.create(ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY);

        assertEquals(IDEMPOTENCY_KEY, payment.getIdempotencyKey());
    }

    @Test
    @DisplayName("Deve rejeitar amount zero")
    void shouldRejectZeroAmount() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, PAYER, RECEIVER, BigDecimal.ZERO, DESCRIPTION, IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Deve rejeitar amount negativo")
    void shouldRejectNegativeAmount() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, PAYER, RECEIVER, new BigDecimal("-1.00"), DESCRIPTION, IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Deve rejeitar payerKey igual a receiverKey")
    void shouldRejectPayerEqualToReceiver() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, PAYER, PAYER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Deve rejeitar payerKey vazio")
    void shouldRejectBlankPayerKey() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, "  ", RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Deve rejeitar receiverKey vazio")
    void shouldRejectBlankReceiverKey() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, PAYER, "  ", AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Deve rejeitar idempotencyKey vazia")
    void shouldRejectBlankIdempotencyKey() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, "  "));
    }

    @Test
    @DisplayName("Deve rejeitar id nulo")
    void shouldRejectNullId() {
        assertThrows(DomainException.class, () ->
                PixPayment.create(null, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY));
    }
}
