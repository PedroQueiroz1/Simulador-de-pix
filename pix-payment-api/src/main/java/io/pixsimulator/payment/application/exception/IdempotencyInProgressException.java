package io.pixsimulator.payment.application.exception;

/**
 * Lancada quando chega uma requisicao com uma {@code Idempotency-Key} cuja
 * operacao original ainda esta {@code PROCESSING} (mesmo payload, ainda nao
 * concluida).
 *
 * O cliente deve aguardar e tentar novamente depois. O
 * {@code RestExceptionHandler} converte esta excecao em HTTP 409 Conflict.
 */
public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String idempotencyKey) {
        super("A request with this Idempotency-Key is still being processed: " + idempotencyKey);
    }
}
