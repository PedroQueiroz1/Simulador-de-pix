package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.entity.OutboxEventEntity;
import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;

/**
 * Mapper manual entre o modelo de aplicacao {@link OutboxEvent} e a entidade JPA
 * {@link OutboxEventEntity}.
 *
 * <p>Intencionalmente explicito (sem MapStruct), deixando clara a fronteira
 * entre aplicacao e persistencia. Nao cria regras novas: apenas converte
 * estruturas. A unica traducao de tipo e o enum {@link OutboxEventStatus}
 * &harr; {@code String} (nome do enum).
 */
public final class OutboxEventJpaMapper {

    private OutboxEventJpaMapper() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /** Converte o modelo de aplicacao para a entidade JPA (caminho de escrita). */
    public static OutboxEventEntity toEntity(OutboxEvent event) {
        return new OutboxEventEntity(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getEventVersion(),
                event.getTopic(),
                event.getPartitionKey(),
                event.getPayload(),
                event.getStatus().name(),
                event.getAttempts(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getPublishedAt(),
                event.getAvailableAt()
        );
    }

    /** Reconstroi o modelo de aplicacao a partir da entidade JPA (caminho de leitura). */
    public static OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.restore(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getEventVersion(),
                entity.getTopic(),
                entity.getPartitionKey(),
                entity.getPayload(),
                OutboxEventStatus.valueOf(entity.getStatus()),
                entity.getAttempts(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getAvailableAt()
        );
    }
}
