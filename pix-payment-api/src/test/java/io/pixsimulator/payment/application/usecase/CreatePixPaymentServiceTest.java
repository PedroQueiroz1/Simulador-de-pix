package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseData;
import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.IdempotencyStartResult;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
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
import java.util.Optional;
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
 * Testes do orquestrador de criacao de pagamento.
 *
 * <p>Usa um {@link RequestFingerprintGenerator} real (deterministico) e mocks do
 * {@link CreatePixPaymentWriter} (passo transacional), do
 * {@link PixPaymentRepository} (recuperacao) e do {@link IdempotencyService},
 * para exercitar os caminhos: primeira requisicao, retry equivalente, conflito
 * de payload, operacao em processamento e a recuperacao da corrida barrada pela
 * constraint unica do banco (ultima barreira).
 */
@ExtendWith(MockitoExtension.class)
class CreatePixPaymentServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    @Mock
    private CreatePixPaymentWriter writer;

    @Mock
    private PixPaymentRepository repository;

    @Mock
    private IdempotencyService idempotencyService;

    private final RequestFingerprintGenerator fingerprintGenerator = new RequestFingerprintGenerator();

    private CreatePixPaymentService service() {
        return new CreatePixPaymentService(writer, repository, fingerprintGenerator, idempotencyService);
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

    private CreatePixPaymentResult writtenResult() {
        return new CreatePixPaymentResult(
                FIXED_ID,
                PixPaymentStatus.CREATED,
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste"
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

    /** Pagamento ja persistido pelo "vencedor" da corrida, com o mesmo payload. */
    private PixPayment existingPayment() {
        return PixPayment.create(
                FIXED_ID,
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                IDEMPOTENCY_KEY
        );
    }

    @Test
    @DisplayName("Primeira criacao deve delegar ao writer transacional e devolver o resultado")
    void firstRequestDelegatesToWriter() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class))).thenReturn(writtenResult());

        CreatePixPaymentResult result = service().create(validCommand());

        assertEquals(FIXED_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());

        ArgumentCaptor<CreatePixPaymentCommand> captor =
                ArgumentCaptor.forClass(CreatePixPaymentCommand.class);
        verify(writer).createNew(captor.capture());
        assertEquals(IDEMPOTENCY_KEY, captor.getValue().idempotencyKey());
    }

    @Test
    @DisplayName("Primeira requisicao deve completar a idempotencia com a resposta original")
    void firstRequestCompletesIdempotency() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class))).thenReturn(writtenResult());

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
    @DisplayName("Retry idempotente equivalente nao deve criar nada de novo")
    void retrySamePayloadDoesNotCreateAgain() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.completed(storedResponse()));

        service().create(validCommand());

        verify(writer, never()).createNew(any(CreatePixPaymentCommand.class));
        verify(idempotencyService, never()).complete(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Mesma chave com payload diferente deve lancar IdempotencyConflictException")
    void differentPayloadThrowsConflict() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenThrow(new IdempotencyConflictException(IDEMPOTENCY_KEY));

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyConflictException.class, () -> service.create(command));
        verify(writer, never()).createNew(any(CreatePixPaymentCommand.class));
    }

    @Test
    @DisplayName("Mesma chave em processamento deve lancar IdempotencyInProgressException")
    void inProgressThrowsInProgress() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenThrow(new IdempotencyInProgressException(IDEMPOTENCY_KEY));

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyInProgressException.class, () -> service.create(command));
        verify(writer, never()).createNew(any(CreatePixPaymentCommand.class));
    }

    // ----------------------------------------------------------------------
    // Recuperacao da corrida barrada pela constraint unica (ultima barreira)
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Constraint unica disparada com payload equivalente deve devolver o pagamento vencedor")
    void dbBarrierWithEquivalentPayloadRecoversWinner() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class)))
                .thenThrow(new DuplicateIdempotencyKeyException(IDEMPOTENCY_KEY, new RuntimeException("dup")));
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingPayment()));

        CreatePixPaymentResult result = service().create(validCommand());

        // Mesma semantica do retry equivalente: resposta do vencedor, sem erro.
        assertEquals(FIXED_ID, result.paymentId());
        assertEquals(PixPaymentStatus.CREATED, result.status());
        assertEquals("11111111111", result.payerKey());
        assertEquals(new BigDecimal("150.75"), result.amount());
    }

    @Test
    @DisplayName("Recuperacao deve completar a idempotencia com a resposta do vencedor")
    void dbBarrierRecoveryCompletesIdempotency() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class)))
                .thenThrow(new DuplicateIdempotencyKeyException(IDEMPOTENCY_KEY, new RuntimeException("dup")));
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingPayment()));

        service().create(validCommand());

        // Cura a chave no Redis: futuros retries equivalentes nem chegam ao banco.
        ArgumentCaptor<IdempotencyResponseData> captor = ArgumentCaptor.forClass(IdempotencyResponseData.class);
        verify(idempotencyService).complete(eq(IDEMPOTENCY_KEY), anyString(), captor.capture());
        assertEquals(FIXED_ID, captor.getValue().paymentId());
    }

    @Test
    @DisplayName("Constraint unica disparada com payload divergente deve lancar IdempotencyConflictException")
    void dbBarrierWithDifferentPayloadThrowsConflict() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class)))
                .thenThrow(new DuplicateIdempotencyKeyException(IDEMPOTENCY_KEY, new RuntimeException("dup")));
        // Vencedor persistiu OUTRO valor para a mesma chave: reuso indevido.
        PixPayment divergent = PixPayment.create(
                FIXED_ID, "11111111111", "22222222222",
                new BigDecimal("999.99"), "Pagamento de teste", IDEMPOTENCY_KEY);
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(divergent));

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyConflictException.class, () -> service.create(command));
        verify(idempotencyService, never()).complete(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Constraint unica disparada com vencedor revertido deve lancar IdempotencyInProgressException")
    void dbBarrierWithRolledBackWinnerThrowsInProgress() {
        when(idempotencyService.startOrReturn(eq(IDEMPOTENCY_KEY), anyString()))
                .thenReturn(IdempotencyStartResult.newOperation());
        when(writer.createNew(any(CreatePixPaymentCommand.class)))
                .thenThrow(new DuplicateIdempotencyKeyException(IDEMPOTENCY_KEY, new RuntimeException("dup")));
        // Caso raro: quem nos barrou sofreu rollback; nao ha registro para devolver.
        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

        CreatePixPaymentService service = service();
        CreatePixPaymentCommand command = validCommand();
        assertThrows(IdempotencyInProgressException.class, () -> service.create(command));
        verify(idempotencyService, never()).complete(anyString(), anyString(), any());
    }
}
