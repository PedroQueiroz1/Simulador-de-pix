package io.pixsimulator.payment.application.dto;

import java.math.BigDecimal;

/**
 * Comando de entrada do caso de uso de criacao de pagamento.
 *
 * E o contrato interno da aplicacao, desacoplado do HTTP. O adapter web
 * converte a request + o header {@code Idempotency-Key} neste comando.
 */
public record CreatePixPaymentCommand(
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description,
        String idempotencyKey
) {
}
