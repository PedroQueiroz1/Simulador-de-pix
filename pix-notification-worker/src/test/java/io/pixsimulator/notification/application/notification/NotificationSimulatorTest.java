package io.pixsimulator.notification.application.notification;

import io.pixsimulator.notification.application.dto.PaymentEventMessage;
import io.pixsimulator.notification.domain.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link NotificationSimulator} (Lote 7).
 *
 * <p>Garante que a mensagem simulada e construida para cada tipo de evento e
 * inclui dados relevantes do pagamento. O simulador nao faz I/O externo, entao o
 * teste e puramente deterministico.
 */
class NotificationSimulatorTest {

    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final UUID LEDGER_TX_ID = UUID.fromString("0197b111-2222-7333-8444-555566667777");

    private final NotificationSimulator simulator = new NotificationSimulator();

    private PaymentEventMessage event(NotificationEventType type,
                                      UUID ledgerTransactionId,
                                      String rejectionReason) {
        return new PaymentEventMessage(
                UUID.randomUUID(), type, 1, LocalDateTime.now(), PAYMENT_ID,
                type.name().replace("PAYMENT_", ""), "11111111111", "22222222222",
                new BigDecimal("150.75"), "Pagamento de teste", LocalDateTime.now(),
                LocalDateTime.now(), ledgerTransactionId, rejectionReason, "corr-123");
    }

    @Test
    @DisplayName("Deve simular notificacao para pagamento criado")
    void simulatesForPaymentCreated() {
        String message = simulator.simulate(event(NotificationEventType.PAYMENT_CREATED, null, null));

        assertNotNull(message);
        assertTrue(message.contains(PAYMENT_ID.toString()), "deve citar o paymentId");
        assertTrue(message.contains("criado"), "deve indicar criacao");
    }

    @Test
    @DisplayName("Deve simular notificacao para pagamento aprovado")
    void simulatesForPaymentApproved() {
        String message = simulator.simulate(event(NotificationEventType.PAYMENT_APPROVED, LEDGER_TX_ID, null));

        assertNotNull(message);
        assertTrue(message.contains("APROVADO"), "deve indicar aprovacao");
        assertTrue(message.contains(LEDGER_TX_ID.toString()), "deve citar o ledgerTransactionId");
    }

    @Test
    @DisplayName("Deve simular notificacao para pagamento rejeitado")
    void simulatesForPaymentRejected() {
        String message = simulator.simulate(
                event(NotificationEventType.PAYMENT_REJECTED, null, "Saldo insuficiente"));

        assertNotNull(message);
        assertTrue(message.contains("REJEITADO"), "deve indicar rejeicao");
        assertTrue(message.contains("Saldo insuficiente"), "deve citar o motivo da rejeicao");
    }
}
