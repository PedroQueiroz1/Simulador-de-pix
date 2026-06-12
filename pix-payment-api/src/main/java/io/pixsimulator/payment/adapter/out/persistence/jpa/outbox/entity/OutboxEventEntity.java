package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA da tabela {@code outbox_events}.
 *
 * <p>Detalhe de infraestrutura, separado do modelo de aplicacao
 * {@code application.outbox.OutboxEvent}. So aqui vivem as anotacoes JPA. O
 * {@code status} e persistido como {@code String} (nome do enum), nunca como
 * ordinal.
 *
 * <p>Ao contrario das entidades append-only do Ledger, esta entidade e
 * <strong>mutavel</strong>: o publisher altera {@code status}, {@code attempts},
 * {@code lastError}, {@code publishedAt} e {@code availableAt} ao longo das
 * tentativas de publicacao. Os campos mutaveis tem setters; os imutaveis (id,
 * agregado, contrato do evento, payload) so chegam pelo construtor.
 *
 * <p>O {@code payload} e {@code NVARCHAR(MAX)} ({@link Nationalized} + tamanho
 * maximo), comportando o JSON do evento. O mapeamento deve bater com o schema
 * criado pelo Flyway (V4), pois o Hibernate roda com {@code ddl-auto=validate}.
 * A unique {@code (aggregate_id, event_type)} (criada como UNIQUE INDEX na V4)
 * e declarada aqui apenas para documentar a intencao.
 */
@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_outbox_events_aggregate_event_type",
                        columnNames = {"aggregate_id", "event_type"}
                )
        }
)
public class OutboxEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 120)
    private String partitionKey;

    @Nationalized
    @Column(name = "payload", nullable = false, length = Integer.MAX_VALUE)
    private String payload;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    /** Construtor sem argumentos exigido pelo JPA/Hibernate. */
    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(UUID id,
                             String aggregateType,
                             UUID aggregateId,
                             String eventType,
                             int eventVersion,
                             String topic,
                             String partitionKey,
                             String payload,
                             String status,
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

    public String getStatus() {
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

    // Campos mutaveis ao longo do ciclo de publicacao.

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setAvailableAt(LocalDateTime availableAt) {
        this.availableAt = availableAt;
    }
}
