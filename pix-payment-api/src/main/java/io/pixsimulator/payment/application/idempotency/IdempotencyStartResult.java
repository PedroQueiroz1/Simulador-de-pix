package io.pixsimulator.payment.application.idempotency;

import java.util.Optional;

/**
 * Resultado de {@link IdempotencyService#startOrReturn(String, String)}.
 *
 *  Modela as duas saidas possiveis quando NAO ha conflito:
 *   {@link #newOperation()}: nao havia registro; a chave foi marcada como
 *   {@code PROCESSING} e o caso de uso deve criar o pagamento;</li>
 *   {@link #completed(IdempotencyResponseData)}: ja havia um registro
 *   {@code COMPLETED} com o mesmo hash; o caso de uso deve apenas devolver a
 *    resposta armazenada (retry equivalente).</li>
 * Os casos de conflito (payload diferente ou operacao em andamento) nao sao
 * representados aqui: viram excecoes lancadas pelo {@link IdempotencyService}.
 */
public final class IdempotencyStartResult {

    private final IdempotencyResponseData existingResponse;

    private IdempotencyStartResult(IdempotencyResponseData existingResponse) {
        this.existingResponse = existingResponse;
    }

    public static IdempotencyStartResult newOperation() {
        return new IdempotencyStartResult(null);
    }

    public static IdempotencyStartResult completed(IdempotencyResponseData existingResponse) {
        return new IdempotencyStartResult(existingResponse);
    }

    /** {@code true} se ja existe uma resposta armazenada para devolver. */
    public boolean isCompleted() {
        return existingResponse != null;
    }

    /** Resposta original, presente apenas em retry equivalente. */
    public Optional<IdempotencyResponseData> response() {
        return Optional.ofNullable(existingResponse);
    }
}
