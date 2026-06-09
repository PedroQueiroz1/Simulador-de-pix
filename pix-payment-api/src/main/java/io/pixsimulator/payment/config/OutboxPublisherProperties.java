package io.pixsimulator.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do publisher da Outbox (prefixo {@code pix.outbox.publisher}),
 * Lote 6.
 *
 * <p>Todos os valores vem de variaveis de ambiente (ver {@code application.yml}
 * e {@code .env.example}) com defaults seguros para desenvolvimento local.
 */
@ConfigurationProperties(prefix = "pix.outbox.publisher")
public class OutboxPublisherProperties {

    /** Liga/desliga o publisher assincrono. Padrao: true. */
    private boolean enabled = true;

    /** Intervalo entre varreduras da Outbox, em milissegundos. Padrao: 5000. */
    private long fixedDelayMs = 5000;

    /** Quantidade maxima de eventos lidos por varredura. Padrao: 20. */
    private int batchSize = 20;

    /** Tentativas de publicacao antes de marcar o evento como FAILED. Padrao: 5. */
    private int maxAttempts = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
