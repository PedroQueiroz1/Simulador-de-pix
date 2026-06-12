package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.GetPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de consulta de pagamento por id.
 */
@ExtendWith(MockitoExtension.class)
class GetPixPaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 6, 10, 30, 0);

    @Mock
    private PixPaymentRepository repository;

    private GetPixPaymentService service() {
        return new GetPixPaymentService(repository);
    }

    private PixPayment existingPayment() {
        return PixPayment.restore(
                PAYMENT_ID,
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321",
                PixPaymentStatus.CREATED,
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve retornar pagamento existente")
    void shouldReturnExistingPayment() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.of(existingPayment()));

        GetPixPaymentResult result = service().getById(PAYMENT_ID);

        assertEquals(PAYMENT_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());
        assertEquals("11111111111", result.payerKey());
        assertEquals("22222222222", result.receiverKey());
        assertEquals(new BigDecimal("150.75"), result.amount());
        assertEquals("Pagamento de teste", result.description());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(CREATED_AT, result.updatedAt());
        assertEquals(null, result.processedAt());
        assertEquals(null, result.rejectionReason());
    }

    @Test
    @DisplayName("Deve lancar PaymentNotFoundException quando pagamento nao existir")
    void shouldThrowWhenPaymentDoesNotExist() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        GetPixPaymentService service = service();
        assertThrows(PaymentNotFoundException.class, () -> service.getById(PAYMENT_ID));
    }
}
