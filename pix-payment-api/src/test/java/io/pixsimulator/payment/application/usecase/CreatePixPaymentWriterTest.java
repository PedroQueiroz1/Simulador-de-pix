package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.exception.DomainException;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do passo transacional da criacao ({@link CreatePixPaymentWriter}):
 * criacao da entidade de dominio, persistencia, gravacao do evento
 * {@code PAYMENT_CREATED} na Outbox e propagacao das excecoes (regra de dominio
 * violada e constraint unica de idempotency_key).
 */
@ExtendWith(MockitoExtension.class)
class CreatePixPaymentWriterTest {

    private static final UUID FIXED_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    @Mock
    private PixPaymentRepository repository;

    @Mock
    private PaymentOutboxEventService paymentOutboxEventService;

    /** Gerador de ID deterministico para testes (substitui o UUIDv7 real). */
    private final IdGenerator idGenerator = () -> FIXED_ID;

    private CreatePixPaymentWriter writer() {
        return new CreatePixPaymentWriter(repository, idGenerator, paymentOutboxEventService);
    }

    private CreatePixPaymentCommand validCommand() {
        return new CreatePixPaymentCommand(
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                IDEMPOTENCY_KEY
        );
    }

    @Test
    @DisplayName("Deve criar o pagamento com id gerado, persistir e devolver o resultado")
    void createsAndPersistsPayment() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePixPaymentResult result = writer().createNew(validCommand());

        assertEquals(FIXED_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());
        assertEquals("11111111111", result.payerKey());
        assertEquals("22222222222", result.receiverKey());
        assertEquals(new BigDecimal("150.75"), result.amount());
        assertEquals("Pagamento de teste", result.description());

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());
        assertEquals(FIXED_ID, captor.getValue().getId());
        assertEquals(IDEMPOTENCY_KEY, captor.getValue().getIdempotencyKey());
    }

    @Test
    @DisplayName("Deve gravar o evento PAYMENT_CREATED na Outbox para o pagamento salvo")
    void recordsOutboxEventForSavedPayment() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        writer().createNew(validCommand());

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(paymentOutboxEventService).recordPaymentCreated(captor.capture());
        assertEquals(FIXED_ID, captor.getValue().getId());
    }

    @Test
    @DisplayName("Deve propagar DomainException para regra violada (payer == receiver) sem salvar")
    void propagatesDomainException() {
        CreatePixPaymentWriter writer = writer();
        CreatePixPaymentCommand invalid = new CreatePixPaymentCommand(
                "11111111111",
                "11111111111", // payer == receiver: viola regra de dominio
                new BigDecimal("150.75"),
                "Pagamento invalido",
                IDEMPOTENCY_KEY
        );

        assertThrows(DomainException.class, () -> writer.createNew(invalid));
        verify(repository, never()).save(any(PixPayment.class));
        verify(paymentOutboxEventService, never()).recordPaymentCreated(any());
    }

    @Test
    @DisplayName("Constraint unica disparada no save deve propagar sem gravar evento de Outbox")
    void propagatesDuplicateKeyWithoutOutboxEvent() {
        when(repository.save(any(PixPayment.class)))
                .thenThrow(new DuplicateIdempotencyKeyException(IDEMPOTENCY_KEY, new RuntimeException("dup")));

        CreatePixPaymentWriter writer = writer();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(DuplicateIdempotencyKeyException.class, () -> writer.createNew(command));
        // O evento nunca pode existir sem o pagamento correspondente.
        verify(paymentOutboxEventService, never()).recordPaymentCreated(any());
    }
}
