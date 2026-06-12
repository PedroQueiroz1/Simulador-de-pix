package io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.repository;

import io.pixsimulator.payment.adapter.out.persistence.jpa.outbox.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link OutboxEventEntity}.
 *
 * <p>A query derivada busca eventos por {@code status} cujo {@code availableAt}
 * ja passou, em ordem de criacao — exatamente o padrao de varredura do
 * publisher, coberto pelo indice {@code IX_outbox_events_status_available_at}.
 * O {@link Pageable} limita o tamanho do lote.
 *
 * <p>Detalhe de infraestrutura: a aplicacao depende apenas da porta
 * {@code OutboxRepository}, implementada pelo {@code JpaOutboxRepositoryAdapter}.
 */
public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            String status, LocalDateTime availableAt, Pageable pageable);
}
