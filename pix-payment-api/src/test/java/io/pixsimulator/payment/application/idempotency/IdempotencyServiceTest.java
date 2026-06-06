package io.pixsimulator.payment.application.idempotency;

import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.port.out.IdempotencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String KEY = "abc-123";
    private static final String HASH = "hash-do-payload";
    private static final Duration TTL = Duration.ofSeconds(86400);

    @Mock
    private IdempotencyRepository repository;

    private IdempotencyService service() {
        return new IdempotencyService(repository, TTL);
    }

    private IdempotencyResponseData responseData() {
        return new IdempotencyResponseData(
                UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41"),
                "CREATED",
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste"
        );
    }

    private IdempotencyRecord processing(String hash) {
        return new IdempotencyRecord(KEY, hash, IdempotencyStatus.PROCESSING, null, LocalDateTime.now(), null);
    }

    private IdempotencyRecord completed(String hash, IdempotencyResponseData response) {
        return new IdempotencyRecord(KEY, hash, IdempotencyStatus.COMPLETED, response,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Chave inexistente deve marcar como PROCESSING e devolver nova operacao")
    void unknownKeyMarksProcessing() {
        when(repository.findByKey(KEY)).thenReturn(Optional.empty());

        IdempotencyStartResult result = service().startOrReturn(KEY, HASH);

        assertFalse(result.isCompleted());
        verify(repository).saveProcessing(KEY, HASH, TTL);
    }

    @Test
    @DisplayName("COMPLETED com mesmo hash deve devolver a resposta armazenada")
    void completedSameHashReturnsStoredResponse() {
        IdempotencyResponseData stored = responseData();
        when(repository.findByKey(KEY)).thenReturn(Optional.of(completed(HASH, stored)));

        IdempotencyStartResult result = service().startOrReturn(KEY, HASH);

        assertTrue(result.isCompleted());
        assertEquals(stored, result.response().orElseThrow());
    }

    @Test
    @DisplayName("COMPLETED com hash diferente deve lancar IdempotencyConflictException")
    void completedDifferentHashThrowsConflict() {
        when(repository.findByKey(KEY)).thenReturn(Optional.of(completed("outro-hash", responseData())));

        IdempotencyService service = service();
        assertThrows(IdempotencyConflictException.class, () -> service.startOrReturn(KEY, HASH));
    }

    @Test
    @DisplayName("PROCESSING com mesmo hash deve lancar IdempotencyInProgressException")
    void processingSameHashThrowsInProgress() {
        when(repository.findByKey(KEY)).thenReturn(Optional.of(processing(HASH)));

        IdempotencyService service = service();
        assertThrows(IdempotencyInProgressException.class, () -> service.startOrReturn(KEY, HASH));
    }

    @Test
    @DisplayName("PROCESSING com hash diferente deve lancar IdempotencyConflictException")
    void processingDifferentHashThrowsConflict() {
        when(repository.findByKey(KEY)).thenReturn(Optional.of(processing("outro-hash")));

        IdempotencyService service = service();
        assertThrows(IdempotencyConflictException.class, () -> service.startOrReturn(KEY, HASH));
    }

    @Test
    @DisplayName("Completar operacao deve salvar COMPLETED com a resposta original")
    void completeSavesCompletedWithResponse() {
        IdempotencyResponseData response = responseData();

        service().complete(KEY, HASH, response);

        verify(repository).saveCompleted(KEY, HASH, response, TTL);
    }

    @Test
    @DisplayName("Deve usar o TTL configurado ao salvar")
    void usesConfiguredTtl() {
        Duration customTtl = Duration.ofSeconds(3600);
        IdempotencyService service = new IdempotencyService(repository, customTtl);
        when(repository.findByKey(KEY)).thenReturn(Optional.empty());

        service.startOrReturn(KEY, HASH);

        verify(repository).saveProcessing(eq(KEY), eq(HASH), eq(customTtl));
    }
}
