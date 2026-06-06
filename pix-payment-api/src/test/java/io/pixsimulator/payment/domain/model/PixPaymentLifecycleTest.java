package io.pixsimulator.payment.domain.model;

import io.pixsimulator.payment.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do ciclo de vida do pagamento (Lote 4): transicoes validas, transicoes
 * proibidas, status terminal e preenchimento de {@code updatedAt},
 * {@code processedAt} e {@code rejectionReason}.
 *
 * <p>As regras vivem no dominio ({@link PixPayment}); aqui exercitamos os
 * metodos de comportamento diretamente, sem Spring.
 */
class PixPaymentLifecycleTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final String DESCRIPTION = "Pagamento de teste";
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";
    private static final String REASON = "Amount exceeds the simulated approval limit";

    private PixPayment newPayment() {
        return PixPayment.create(ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("Pagamento criado inicia como CREATED, nao terminal e com updatedAt = createdAt")
    void createdPaymentStartsAsCreated() {
        PixPayment payment = newPayment();

        assertEquals(PixPaymentStatus.CREATED, payment.getStatus());
        assertFalse(payment.isTerminal());
        assertNotNull(payment.getCreatedAt());
        assertEquals(payment.getCreatedAt(), payment.getUpdatedAt());
        assertNull(payment.getProcessedAt());
        assertNull(payment.getRejectionReason());
    }

    @Test
    @DisplayName("CREATED pode ir para PROCESSING")
    void createdCanGoToProcessing() {
        PixPayment payment = newPayment();

        payment.markAsProcessing(LocalDateTime.now());

        assertEquals(PixPaymentStatus.PROCESSING, payment.getStatus());
        assertFalse(payment.isTerminal());
    }

    @Test
    @DisplayName("PROCESSING pode ir para APPROVED")
    void processingCanGoToApproved() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());

        payment.approve(LocalDateTime.now());

        assertEquals(PixPaymentStatus.APPROVED, payment.getStatus());
        assertTrue(payment.isTerminal());
    }

    @Test
    @DisplayName("PROCESSING pode ir para REJECTED")
    void processingCanGoToRejected() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());

        payment.reject(REASON, LocalDateTime.now());

        assertEquals(PixPaymentStatus.REJECTED, payment.getStatus());
        assertTrue(payment.isTerminal());
    }

    @Test
    @DisplayName("CREATED nao pode ir direto para APPROVED")
    void createdCannotGoDirectlyToApproved() {
        PixPayment payment = newPayment();

        assertThrows(DomainException.class, () -> payment.approve(LocalDateTime.now()));
        assertEquals(PixPaymentStatus.CREATED, payment.getStatus());
    }

    @Test
    @DisplayName("CREATED nao pode ir direto para REJECTED")
    void createdCannotGoDirectlyToRejected() {
        PixPayment payment = newPayment();

        assertThrows(DomainException.class, () -> payment.reject(REASON, LocalDateTime.now()));
        assertEquals(PixPaymentStatus.CREATED, payment.getStatus());
    }

    @Test
    @DisplayName("APPROVED nao pode mudar para outro status")
    void approvedCannotChange() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());
        payment.approve(LocalDateTime.now());

        assertThrows(DomainException.class, () -> payment.markAsProcessing(LocalDateTime.now()));
        assertThrows(DomainException.class, () -> payment.reject(REASON, LocalDateTime.now()));
        assertThrows(DomainException.class, () -> payment.approve(LocalDateTime.now()));
        assertEquals(PixPaymentStatus.APPROVED, payment.getStatus());
    }

    @Test
    @DisplayName("REJECTED nao pode mudar para outro status")
    void rejectedCannotChange() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());
        payment.reject(REASON, LocalDateTime.now());

        assertThrows(DomainException.class, () -> payment.markAsProcessing(LocalDateTime.now()));
        assertThrows(DomainException.class, () -> payment.approve(LocalDateTime.now()));
        assertThrows(DomainException.class, () -> payment.reject(REASON, LocalDateTime.now()));
        assertEquals(PixPaymentStatus.REJECTED, payment.getStatus());
    }

    @Test
    @DisplayName("Ao aprovar, preenche processedAt")
    void approveFillsProcessedAt() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());
        LocalDateTime processedAt = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

        payment.approve(processedAt);

        assertEquals(processedAt, payment.getProcessedAt());
        assertNull(payment.getRejectionReason());
    }

    @Test
    @DisplayName("Ao rejeitar, preenche processedAt e rejectionReason")
    void rejectFillsProcessedAtAndReason() {
        PixPayment payment = newPayment();
        payment.markAsProcessing(LocalDateTime.now());
        LocalDateTime processedAt = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

        payment.reject(REASON, processedAt);

        assertEquals(processedAt, payment.getProcessedAt());
        assertEquals(REASON, payment.getRejectionReason());
    }

    @Test
    @DisplayName("Ao mudar status, atualiza updatedAt")
    void transitionUpdatesUpdatedAt() {
        PixPayment payment = newPayment();
        LocalDateTime createdUpdatedAt = payment.getUpdatedAt();

        LocalDateTime processingAt = createdUpdatedAt.plusMinutes(1);
        payment.markAsProcessing(processingAt);
        assertEquals(processingAt, payment.getUpdatedAt());

        LocalDateTime approvedAt = createdUpdatedAt.plusMinutes(2);
        payment.approve(approvedAt);
        assertEquals(approvedAt, payment.getUpdatedAt());
    }
}
