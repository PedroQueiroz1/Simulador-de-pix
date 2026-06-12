package io.pixsimulator.payment.application.exception;

/**
 * Lancada quando se tenta processar um pagamento que ja esta em status terminal
 * ({@code APPROVED} ou {@code REJECTED}).
 *
 * O ciclo de vida nao permite reprocessar um pagamento ja decidido. O
 * {@code RestExceptionHandler} converte esta excecao em HTTP 409 Conflict.
 */
public class PaymentNotProcessableException extends RuntimeException {

    public PaymentNotProcessableException() {
        super("Payment is already in a terminal status");
    }
}
