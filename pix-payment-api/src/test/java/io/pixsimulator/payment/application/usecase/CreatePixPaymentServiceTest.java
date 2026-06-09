package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseData;
import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.IdempotencyStartResult;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de criacao de pagamento ja na versao Lote 3, com
 * idempotencia delegada ao {@link IdempotencyService}.
 *
 * <p>Usa um {@link RequestFingerprintGenerator} real (deterministico) e mocks de
 * {@link PixPaymentRepository} e {@link IdempotencyService}, para exercitar os
 * caminhos: primeira requisicao, retry equivalente, conflito de payload e
 * operacao em processamento.
 */
@ExtendWith(MockitoExtension.class)
class CreatePixPaymentServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    @Mock
    private PixPaymentRepository repository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentOutboxEventService paymentOutboxEventService;

    private final RequestFingerprintGenerator fingerprintGenerator = new RequestFingerprintGenerator();

    /** Gerador de ID deterministico para testes (substitui o UUIDv7 real). */
    private final IdGenerator idGenerator = () -> FIXED_ID;

    private CreatePixPaymentService service() {
        return new CreatePixPaymentService(
                repository, idGenerator, fingerprintGenerator, idempotencyService, paymentOutboxEventService);
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

    private IdempotencyResponseData storedResponse() {
        return new IdempotencyResponseData(
                FIXED_ID,
                PixPaymentStatus.CREATED.name(),
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste"
        );
    }

    @Test
    @DisplayName("Primeira criacao deve salvar Payment e OutboxEvent PAYMENT_CREATED")
    void firstRequestCreatesPayment() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePixPaymentResult result = service().create(validCommand());

        assertEquals(FIXED_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());
        assertEquals(FIXED_ID, captor.getValue().getId());
        assertEquals(IDEMPOTENCY_KEY, captor.getValue().getIdempotencyKey());

        // Lote 6: o evento PAYMENT_CREATED e gravado na mesma transacao.
        ArgumentCaptor<PixPayment> outboxCaptor = ArgumentCaptor.forClass(PixPayment.class);
        verify(paymentOutboxEventService).recordPaymentCreated(outboxCaptor.capture());
        assertEquals(FIXED_ID, outboxCaptor.getValue().getId());
    }

    @Test
    @DisplayName("Primeira requisicao deve completar a idempotencia com a resposta original")
    void firstRequestCompletesIdempotency() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().create(validCommand());

        ArgumentCaptor<IdempotencyResponseData> captor = ArgumentCaptor.forClass(IdempotencyResponseData.class);
        verify(idempotencyService).complete(eq(IDEMPOTENCY_KEY), anyString(), captor.capture());
        assertEquals(FIXED_ID, captor.getValue().paymentId());
        assertEquals(PixPaymentStatus.CREATED.name(), captor.getValue().status());
    }

    @Test
    @DisplayName("Retry com mesma chave e mesmo payload deve retornar a resposta armazenada")
    void retrySamePayloadReturnsStoredResponse() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.completed(storedResponse()));

        CreatePixPaymentResult result = service().create(validCommand());

        assertEquals(FIXED_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());
        assertEquals("11111111111", result.payerKey());
        assertEquals("22222222222", result.receiverKey());
        assertEquals(new BigDecimal("150.75"), result.amount());
        assertEquals("Pagamento de teste", result.description());
    }

    @Test
    @DisplayName("Retry idempotente equivalente nao deve salvar novo OutboxEvent")
    void retrySamePayloadDoesNotSaveAgain() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.completed(storedResponse()));

        service().create(validCommand());

        verify(repository, never()).save(any(PixPayment.class));
        verify(idempotencyService, never()).complete(anyString(), anyString(), any());
        // Lote 6: retry equivalente nao recria evento de Outbox.
        verify(paymentOutboxEventService, never()).recordPaymentCreated(any());
    }

    @Test
    @DisplayName("Mesma chave com payload diferente deve lancar IdempotencyConflictException")
    void differentPayloadThrowsConflict() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenThrow(new IdempotencyConflictException(IDEMPOTENCY_KEY));

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyConflictException.class, () -> service.create(command));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Mesma chave em processamento deve lancar IdempotencyInProgressException")
    void inProgressThrowsInProgress() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenThrow(new IdempotencyInProgressException(IDEMPOTENCY_KEY));

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyInProgressException.class, () -> service.create(command));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Deve propagar DomainException para regra violada (payer == receiver)")
    void propagatesDomainException() {
        when(idempotencyService.startOrReturn(anyString(), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand invalid = new CreatePixPaymentCommand(
                "11111111111",
                "11111111111", // payer == receiver: viola regra de dominio
                new BigDecimal("150.75"),
                "Pagamento invalido",
                IDEMPOTENCY_KEY
        );

        assertThrows(DomainException.class, () -> service.create(invalid));
        verify(repository, never()).save(any(PixPayment.class));
    }
}
