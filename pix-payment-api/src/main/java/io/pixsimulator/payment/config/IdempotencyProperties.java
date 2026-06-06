package io.pixsimulator.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propriedades de configuracao da idempotencia (prefixo {@code pix.idempotency}).
 *
 * <p>Hoje expoe apenas o TTL dos registros de idempotencia no Redis. O valor
 * padrao (24h) cobre a maioria dos retries; pode ser ajustado por ambiente sem
 * recompilar.
 */
@ConfigurationProperties(prefix = "pix.idempotency")
public class IdempotencyProperties {

    /** Tempo de vida do registro de idempotencia, em segundos. Padrao: 86400 (24h). */
    private long ttlSeconds = 86400;

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /** TTL como {@link Duration}, forma usada pelo {@code IdempotencyService}. */
    public Duration ttl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}
