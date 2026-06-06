package io.pixsimulator.payment.application.exception;

/**
 * Lancada quando uma {@code Idempotency-Key} ja conhecida e reutilizada com um
 * payload diferente do original (hashes divergentes).
 *
 * Nao e um retry seguro: a mesma chave so pode representar a mesma intencao
 * de pagamento. O {@code RestExceptionHandler} converte esta excecao em
 * HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key reused with a different payload: " + idempotencyKey);
    }
}
