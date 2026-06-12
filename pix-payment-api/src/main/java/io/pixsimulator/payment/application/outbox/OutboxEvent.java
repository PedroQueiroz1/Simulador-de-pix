package io.pixsimulator.payment.application.outbox;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro do Transactional Outbox: um evento gravado na tabela
 * {@code outbox_events} na MESMA transacao da mudanca de estado de negocio.
 *
 * <p>Vive na camada de aplicacao (e nao no dominio): o dominio
 * ({@code domain.*}) continua sem conhecer Outbox, Kafka ou JSON (ADR-025 /
 * spec 027). Esta classe nao depende de Spring nem de JPA — o {@code payload} ja
 * chega como JSON pronto (string) e o {@code id} ja chega gerado por uma porta
 * {@code IdGenerator}.
 *
 * <p>Carrega o proprio estado de publicacao ({@link OutboxEventStatus}) e a
 * politica de tentativas: {@link #markPublished(LocalDateTime)} no sucesso e
 * {@link #registerFailedAttempt(String, LocalDateTime, int)} na falha, que
 * incrementa {@code attempts}, guarda uma mensagem de erro curta e so marca como
 * {@link OutboxEventStatus#FAILED} ao atingir o limite de tentativas.
 */
public final class OutboxEvent {

    /** Limite de tamanho da mensagem de erro persistida (coluna last_error). */
    static final int MAX_ERROR_LENGTH = 1000;

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final int eventVersion;
    private final String topic;
    private final String partitionKey;
    private final String payload;
    private final LocalDateTime createdAt;

    private OutboxEventStatus status;
    private int attempts;
    private String lastError;
    private LocalDateTime publishedAt;
    private LocalDateTime availableAt;

    private OutboxEvent(UUID id,
                        String aggregateType,
                        UUID aggregateId,
                        String eventType,
                        int eventVersion,
                        String topic,
                        String partitionKey,
                        String payload,
                        OutboxEventStatus status,
                        int attempts,
                        String lastError,
                        LocalDateTime createdAt,
                        LocalDateTime publishedAt,
                        LocalDateTime availableAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.availableAt = availableAt;
    }

    /**
     * Cria um novo evento {@link OutboxEventStatus#PENDING}, disponivel para
     * publicacao imediatamente ({@code availableAt = now}), com {@code attempts}
     * em zero e sem {@code publishedAt}.
     *
     * @throws IllegalArgumentException se algum campo obrigatorio for invalido
     *         (id/aggregateId nulos; aggregateType/eventType/topic/partitionKey/
     *         payload em branco; eventVersion menor que 1; now nulo).
     */
    public static OutboxEvent create(UUID id,
                                     String aggregateType,
                                     UUID aggregateId,
                                     String eventType,
                                     int eventVersion,
                                     String topic,
                                     String partitionKey,
                                     String payload,
                                     LocalDateTime now) {
        requireNonNull(id, "id");
        requireText(aggregateType, "aggregateType");
        requireNonNull(aggregateId, "aggregateId");
        requireText(eventType, "eventType");
        if (eventVersion < 1) {
            throw new IllegalArgumentException("eventVersion must be >= 1");
        }
        requireText(topic, "topic");
        requireText(partitionKey, "partitionKey");
        requireText(payload, "payload");
        requireNonNull(now, "now");

        return new OutboxEvent(
                id,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                topic,
                partitionKey,
                payload,
                OutboxEventStatus.PENDING,
                0,
                null,
                now,
                null,
                now);
    }

    /**
     * Reconstroi um evento ja persistido a partir de todos os seus campos.
     *
     * <p>Usado pelo mapper de persistencia (Entity -&gt; aplicacao). Diferente de
     * {@link #create}, nao reaplica regras de criacao: preserva fielmente o
     * estado salvo (status, attempts, timestamps).
     */
    public static OutboxEvent restore(UUID id,
                                      String aggregateType,
                                      UUID aggregateId,
                                      String eventType,
                                      int eventVersion,
                                      String topic,
                                      String partitionKey,
                                      String payload,
                                      OutboxEventStatus status,
                                      int attempts,
                                      String lastError,
                                      LocalDateTime createdAt,
                                      LocalDateTime publishedAt,
                                      LocalDateTime availableAt) {
        return new OutboxEvent(
                id,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                topic,
                partitionKey,
                payload,
                status,
                attempts,
                lastError,
                createdAt,
                publishedAt,
                availableAt);
    }

    /**
     * Marca o evento como {@link OutboxEventStatus#PUBLISHED}, registrando o
     * instante da publicacao e limpando o ultimo erro.
     */
    public void markPublished(LocalDateTime publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    /**
     * Registra uma tentativa de publicacao que falhou.
     *
     * <p>Incrementa {@code attempts}, guarda uma mensagem de erro curta
     * (truncada em {@value #MAX_ERROR_LENGTH} caracteres — nunca a stack trace
     * inteira) e agenda a proxima janela de disponibilidade
     * ({@code availableAt = nextAvailableAt}). Se {@code attempts} atingir
     * {@code maxAttempts}, o evento vira {@link OutboxEventStatus#FAILED};
     * caso contrario permanece {@link OutboxEventStatus#PENDING} para nova
     * tentativa.
     */
    public void registerFailedAttempt(String error, LocalDateTime nextAvailableAt, int maxAttempts) {
        this.attempts += 1;
        this.lastError = truncateError(error);
        this.availableAt = nextAvailableAt;
        if (this.attempts >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        }
    }

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getTopic() {
        return topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutboxEvent)) {
            return false;
        }
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
