package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de consulta de Ledger por pagamento (Lote 5).
 */
@ExtendWith(MockitoExtension.class)
class GetLedgerByPaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

    @Mock
    private PixPaymentRepository paymentRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    private GetLedgerByPaymentService service() {
        return new GetLedgerByPaymentService(paymentRepository, ledgerRepository);
    }

    private PixPayment approvedPayment() {
        return PixPayment.restore(
                PAYMENT_ID, "11111111111", "22222222222", AMOUNT, "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321",
                PixPaymentStatus.APPROVED, AT, AT, AT, null);
    }

    @Test
    @DisplayName("Deve retornar transacoes quando o pagamento tiver ledger")
    void shouldReturnTransactionsWhenLedgerExists() {
        LedgerTransaction ledger = LedgerTransaction.createPixSettlement(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                PAYMENT_ID, "11111111111", "22222222222", AMOUNT, AT);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
        when(ledgerRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of(ledger));

        GetLedgerByPaymentResult result = service().getByPaymentId(PAYMENT_ID);

        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(1, result.transactions().size());
        assertEquals(LedgerOperationType.PIX_SETTLEMENT, result.transactions().get(0).operationType());
        assertEquals(2, result.transactions().get(0).entries().size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o pagamento existir mas nao tiver ledger")
    void shouldReturnEmptyWhenPaymentHasNoLedger() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
        when(ledgerRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());

        GetLedgerByPaymentResult result = service().getByPaymentId(PAYMENT_ID);

        assertEquals(PAYMENT_ID, result.paymentId());
        assertTrue(result.transactions().isEmpty());
    }

    @Test
    @DisplayName("Deve lancar PaymentNotFoundException quando o pagamento nao existir")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        GetLedgerByPaymentService service = service();
        assertThrows(PaymentNotFoundException.class, () -> service.getByPaymentId(PAYMENT_ID));
    }
}
