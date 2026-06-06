package io.pixsimulator.payment.application.exception;

import java.util.UUID;

/**
 * Lancada quando nao existe pagamento para o {@code paymentId} consultado ou
 * processado (Lote 4).
 *
 * E uma excecao de aplicacao (nao de dominio): a ausencia de um registro e uma
 * condicao de busca no repositorio, nao uma violacao de regra de negocio do
 * objeto. O {@code RestExceptionHandler} a converte em HTTP 404 Not Found.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("No payment was found for the provided paymentId: " + paymentId);
    }
}
