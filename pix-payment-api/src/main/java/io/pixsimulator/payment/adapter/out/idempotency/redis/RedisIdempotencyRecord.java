package io.pixsimulator.payment.adapter.out.idempotency.redis;

/**
 * Forma serializada (JSON no Redis) de um registro de idempotencia.
 *
 * Espelha {@code IdempotencyRecord} da aplicacao, mas isola detalhes de
 * serializacao no adapter: {@code status} e os instantes sao guardados como
 * {@code String} (ISO-8601) para serializar de forma estavel e legivel, sem
 * depender de modulos extras do Jackson para tipos de data.
 *
 * @param response   presente apenas quando {@code status == COMPLETED}.
 * @param completedAt presente apenas quando {@code status == COMPLETED}.
 */
public record RedisIdempotencyRecord(
        String idempotencyKey,
        String requestHash,
        String status,
        RedisIdempotencyResponseData response,
        String createdAt,
        String completedAt
) {
}
