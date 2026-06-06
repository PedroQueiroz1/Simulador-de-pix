package io.pixsimulator.payment.adapter.out.idempotency.redis;

import io.pixsimulator.payment.application.idempotency.IdempotencyRecord;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseData;
import io.pixsimulator.payment.application.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao do adapter Redis usando Redis real via Testcontainers
 * ({@link GenericContainer} com a imagem {@code redis:7-alpine}).
 *
 * <p>Nao sobe o contexto Spring completo (que exigiria SQL Server): monta
 * manualmente a {@code LettuceConnectionFactory}, o {@link RedisTemplate} (via
 * {@link RedisConfig}) e o {@link RedisIdempotencyRepository} apontados para o
 * container. Assim valida serializacao JSON, recuperacao e TTL contra um Redis
 * de verdade, isolado.
 */
@Testcontainers
class RedisIdempotencyRepositoryIntegrationTest {

    private static final Duration TTL = Duration.ofSeconds(100);

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, RedisIdempotencyRecord> redisTemplate;
    private static RedisIdempotencyRepository repository;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new RedisConfig().idempotencyRedisTemplate(connectionFactory);
        repository = new RedisIdempotencyRepository(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    private String uniqueKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private IdempotencyResponseData responseData() {
        return new IdempotencyResponseData(
                UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41"),
                "CREATED",
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste"
        );
    }

    @Test
    @DisplayName("Deve salvar e recuperar um registro PROCESSING")
    void savesAndRetrievesProcessing() {
        String key = uniqueKey("processing");

        repository.saveProcessing(key, "hash-1", TTL);

        Optional<IdempotencyRecord> found = repository.findByKey(key);
        assertTrue(found.isPresent());
        assertEquals(IdempotencyStatus.PROCESSING, found.get().status());
        assertEquals("hash-1", found.get().requestHash());
        assertNull(found.get().response());
    }

    @Test
    @DisplayName("Deve salvar e recuperar a resposta de um registro COMPLETED")
    void savesAndRetrievesCompletedResponse() {
        String key = uniqueKey("completed");
        IdempotencyResponseData response = responseData();

        repository.saveCompleted(key, "hash-2", response, TTL);

        Optional<IdempotencyRecord> found = repository.findByKey(key);
        assertTrue(found.isPresent());
        assertEquals(IdempotencyStatus.COMPLETED, found.get().status());
        assertEquals("hash-2", found.get().requestHash());

        IdempotencyResponseData stored = found.get().response();
        assertNotNull(stored);
        assertEquals(response.paymentId(), stored.paymentId());
        assertEquals(response.status(), stored.status());
        assertEquals(response.payerKey(), stored.payerKey());
        assertEquals(response.receiverKey(), stored.receiverKey());
        assertEquals(0, response.amount().compareTo(stored.amount()));
        assertEquals(response.description(), stored.description());
    }

    @Test
    @DisplayName("Deve respeitar o TTL configurado ao salvar")
    void respectsTtl() {
        String key = uniqueKey("ttl");

        repository.saveProcessing(key, "hash-3", TTL);

        Long expire = redisTemplate.getExpire(RedisIdempotencyRepository.KEY_PREFIX + key);
        assertNotNull(expire);
        assertTrue(expire > 0, "a chave deve ter TTL positivo");
        assertTrue(expire <= TTL.getSeconds(), "o TTL nao deve exceder o configurado");
    }

    @Test
    @DisplayName("Deve retornar vazio para chave inexistente")
    void returnsEmptyForUnknownKey() {
        Optional<IdempotencyRecord> found = repository.findByKey(uniqueKey("inexistente"));

        assertTrue(found.isEmpty());
    }
}
