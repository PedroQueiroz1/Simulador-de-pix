package io.pixsimulator.payment.adapter.out.idempotency.redis;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Forma serializada (JSON no Redis) da resposta idempotente armazenada.
 *
 * Espelha {@code IdempotencyResponseData} da aplicacao, mas vive no adapter
 * para que a forma de serializacao no Redis nao vaze para a camada de
 * aplicacao. O {@link RedisIdempotencyRepository} faz a conversao entre os dois.
 *
 * Mantida como record com construtor sem-args implicito via Jackson: o
 * desserializador do Spring Data Redis reconstroi a partir do JSON.
 */
public record RedisIdempotencyResponseData(
        UUID paymentId,
        String status,
        String payerKey,
        String receiverKey,
        BigDecimal amount,
        String description
) {
}
