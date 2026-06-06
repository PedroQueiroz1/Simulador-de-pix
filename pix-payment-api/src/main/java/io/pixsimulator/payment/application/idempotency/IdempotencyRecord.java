package io.pixsimulator.payment.application.idempotency;

import java.time.LocalDateTime;

/**
 * Registro de idempotencia recuperado da camada de armazenamento (Redis).
 *
 * Representa o estado de uma {@code Idempotency-Key}: o hash do payload que a
 * originou, o status atual e, quando concluida, a resposta original.
 *
 * E um modelo da camada de aplicacao, independente de Redis. O adapter
 * converte sua representacao tecnica (JSON no Redis) para este record antes de
 * devolver ao {@link IdempotencyService}.
 *
 * @param response preenchido apenas quando {@code status == COMPLETED};
 *                 {@code null} enquanto a operacao esta {@code PROCESSING}.
 * @param completedAt preenchido apenas quando {@code status == COMPLETED}.
 */
public record IdempotencyRecord(
        String idempotencyKey,
        String requestHash,
        IdempotencyStatus status,
        IdempotencyResponseData response,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
