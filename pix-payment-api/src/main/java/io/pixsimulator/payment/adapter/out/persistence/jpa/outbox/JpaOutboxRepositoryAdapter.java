package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox;

import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.entity.OutboxEventEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.mapper.OutboxEventJpaMapper;
import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.repository.SpringDataOutboxEventRepository;
import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adapter de saida que implementa a porta {@link OutboxRepository} usando
 * Spring Data JPA + SQL Server (Lote 6).
 *
 * <p>Traduz aplicacao &harr; entity via {@link OutboxEventJpaMapper} e delega o
 * acesso ao banco ao {@link SpringDataOutboxEventRepository}.
 *
 * <p>O {@code save} usa propagacao padrao ({@code REQUIRED}): quando chamado de
 * dentro do caso de uso (criacao/processamento), participa da transacao aberta
 * la, garantindo que a mudanca de estado e o OutboxEvent sejam atomicos
 * (ADR-025). Os metodos do publisher ({@code markAsPublished}/{@code markAsFailed})
 * rodam em transacoes proprias e curtas, uma por evento.
 */
@Repository
public class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataOutboxEventRepository jpaRepository;

    public JpaOutboxRepositoryAdapter(SpringDataOutboxEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        // saveAndFlush forca o INSERT na hora: a unique (aggregate_id, event_type)
        // falha imediatamente (DataIntegrityViolationException), barrando evento
        // duplicado para o mesmo agregado.
        OutboxEventEntity saved = jpaRepository.saveAndFlush(OutboxEventJpaMapper.toEntity(event));
        return OutboxEventJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int limit, LocalDateTime now) {
        return jpaRepository
                .findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING.name(), now, PageRequest.of(0, limit))
                .stream()
                .map(OutboxEventJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void markAsPublished(UUID eventId, LocalDateTime publishedAt) {
        OutboxEventEntity entity = jpaRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
        entity.setStatus(OutboxEventStatus.PUBLISHED.name());
        entity.setPublishedAt(publishedAt);
        entity.setLastError(null);
        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void markAsFailed(OutboxEvent event) {
        // O evento ja chega com o novo estado decidido pelo dominio
        // (attempts incrementado, lastError curto, nova availableAt e status
        // PENDING para retry ou FAILED ao esgotar tentativas). Persistimos via
        // merge (mesmo id => UPDATE).
        jpaRepository.save(OutboxEventJpaMapper.toEntity(event));
    }
}
