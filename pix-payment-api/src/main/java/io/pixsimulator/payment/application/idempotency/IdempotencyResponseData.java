package io.pixsimulator.payment.application.idempotency;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resposta original armazenada para uma operacao idempotente concluida.
 *
 * Guarda apenas o necessario para reconstruir um
 * {@code CreatePixPaymentResult} em um retry equivalente, permitindo devolver
 * exatamente o mesmo {@code paymentId} e os mesmos dados da primeira resposta.
 *
 * O {@code status} e mantido como {@code String} (nome do enum
 * {@code PixPaymentStatus}) para evitar acoplar este DTO de idempotencia ao
 * enum de dominio na serializacao. Nao armazena stack trace nem dados
 * desnecessarios.
 */
public record IdempotencyResponseData(
        UUID paymentId,
        String status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description
) {
}
