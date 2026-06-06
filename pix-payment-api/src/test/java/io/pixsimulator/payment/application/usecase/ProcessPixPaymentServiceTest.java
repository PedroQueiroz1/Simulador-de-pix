package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de processamento simulado de pagamento (Lote 4).
 *
 * <p>A regra e deterministica: {@code amount <= 5000.00} aprova; acima rejeita.
 */
@ExtendWith(MockitoExtension.class)
class ProcessPixPaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 6, 10, 30, 0);

    @Mock
    private PixPaymentRepository repository;

    private ProcessPixPaymentService service() {
        return new ProcessPixPaymentService(repository);
    }

    private PixPayment paymentWith(BigDecimal amount, PixPaymentStatus status) {
        return PixPayment.restore(
                PAYMENT_ID,
                "11111111111",
                "22222222222",
                amount,
                "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321",
                status,
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve aprovar pagamento com amount <= 5000.00")
    void shouldApproveWhenAmountUpToLimit() {
        // Limite e inclusivo: exatamente 5000.00 deve aprovar.
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("5000.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPixPaymentResult result = service().process(PAYMENT_ID);

        assertEquals(PixPaymentStatus.APPROVED, result.status());
        assertNotNull(result.processedAt());
        assertNull(result.rejectionReason());
    }

    @Test
    @DisplayName("Deve rejeitar pagamento com amount > 5000.00")
    void shouldRejectWhenAmountAboveLimit() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("5000.01"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPixPaymentResult result = service().process(PAYMENT_ID);

        assertEquals(PixPaymentStatus.REJECTED, result.status());
        assertNotNull(result.processedAt());
        assertEquals("Amount exceeds the simulated approval limit", result.rejectionReason());
    }

    @Test
    @DisplayName("Deve salvar pagamento atualizado")
    void shouldSaveUpdatedPayment() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().process(PAYMENT_ID);

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());
        assertEquals(PixPaymentStatus.APPROVED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getProcessedAt());
    }

    @Test
    @DisplayName("Deve lancar PaymentNotFoundException quando pagamento nao existir")
    void shouldThrowWhenPaymentDoesNotExist() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotFoundException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando pagamento ja estiver APPROVED")
    void shouldThrowWhenAlreadyApproved() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.APPROVED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando pagamento ja estiver REJECTED")
    void shouldThrowWhenAlreadyRejected() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("9000.00"), PixPaymentStatus.REJECTED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }
}
