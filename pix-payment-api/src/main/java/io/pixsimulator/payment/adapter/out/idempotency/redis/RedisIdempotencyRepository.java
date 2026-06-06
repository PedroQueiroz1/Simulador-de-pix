package io.pixsimulator.payment.adapter.out.idempotency.redis;

import io.pixsimulator.payment.application.idempotency.IdempotencyRecord;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseData;
import io.pixsimulator.payment.application.idempotency.IdempotencyStatus;
import io.pixsimulator.payment.application.port.out.IdempotencyRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementacao Redis da porta {@link IdempotencyRepository}.
 *
 * Armazena cada registro de idempotencia como um valor JSON sob a chave
 * {@code idempotency:pix-payment:{idempotencyKey}}, com TTL. Faz a conversao
 * entre os modelos da aplicacao ({@link IdempotencyRecord} /
 * {@link IdempotencyResponseData}) e suas formas serializadas
 * ({@link RedisIdempotencyRecord} / {@link RedisIdempotencyResponseData}),
 * mantendo a aplicacao livre de detalhes do Redis.
 *
 * {@code RedisTemplate} fica confinado a este adapter: nem o controller nem o
 * caso de uso nem o dominio o conhecem.
 */
@Repository
public class RedisIdempotencyRepository implements IdempotencyRepository {

    /** Prefixo da chave Redis. Publico para uso em testes de integracao. */
    public static final String KEY_PREFIX = "idempotency:pix-payment:";

    private final RedisTemplate<String, RedisIdempotencyRecord> redisTemplate;

    public RedisIdempotencyRepository(RedisTemplate<String, RedisIdempotencyRecord> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        RedisIdempotencyRecord raw = redisTemplate.opsForValue().get(redisKey(idempotencyKey));
        return Optional.ofNullable(raw).map(this::toDomain);
    }

    @Override
    public void saveProcessing(String idempotencyKey, String requestHash, Duration ttl) {
        RedisIdempotencyRecord record = new RedisIdempotencyRecord(
                idempotencyKey,
                requestHash,
                IdempotencyStatus.PROCESSING.name(),
                null,
                LocalDateTime.now().toString(),
                null
        );
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), record, ttl);
    }

    @Override
    public void saveCompleted(String idempotencyKey,
                              String requestHash,
                              IdempotencyResponseData response,
                              Duration ttl) {
        RedisIdempotencyRecord record = new RedisIdempotencyRecord(
                idempotencyKey,
                requestHash,
                IdempotencyStatus.COMPLETED.name(),
                toRedis(response),
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString()
        );
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), record, ttl);
    }

    private String redisKey(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }

    private IdempotencyRecord toDomain(RedisIdempotencyRecord raw) {
        return new IdempotencyRecord(
                raw.idempotencyKey(),
                raw.requestHash(),
                IdempotencyStatus.valueOf(raw.status()),
                toDomain(raw.response()),
                parse(raw.createdAt()),
                parse(raw.completedAt())
        );
    }

    private IdempotencyResponseData toDomain(RedisIdempotencyResponseData raw) {
        if (raw == null) {
            return null;
        }
        return new IdempotencyResponseData(
                raw.paymentId(),
                raw.status(),
                raw.payerKey(),
                raw.receiverKey(),
                raw.amount(),
                raw.description()
        );
    }

    private RedisIdempotencyResponseData toRedis(IdempotencyResponseData response) {
        if (response == null) {
            return null;
        }
        return new RedisIdempotencyResponseData(
                response.paymentId(),
                response.status(),
                response.payerKey(),
                response.receiverKey(),
                response.amount(),
                response.description()
        );
    }

    private LocalDateTime parse(String isoDateTime) {
        return isoDateTime == null ? null : LocalDateTime.parse(isoDateTime);
    }
}
