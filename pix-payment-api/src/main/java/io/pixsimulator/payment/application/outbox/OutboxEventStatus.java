package io.pixsimulator.payment.application.outbox;

/**
 * Estado de publicacao de um {@link OutboxEvent}.
 *
 * <pre>
 *   PENDING   -> aguardando publicacao pelo publisher assincrono.
 *   PUBLISHED -> publicado com sucesso no Kafka.
 *   FAILED    -> esgotou as tentativas de publicacao (max attempts).
 * </pre>
 *
 * <p>O ciclo normal e {@code PENDING -> PUBLISHED}. Em falhas transitorias o
 * evento permanece {@code PENDING} (com {@code attempts} incrementado e nova
 * janela em {@code availableAt}); so vira {@code FAILED} ao atingir o limite de
 * tentativas.
 */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
