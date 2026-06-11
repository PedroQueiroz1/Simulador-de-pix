package io.pixsimulator.notification.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data MongoDB para {@link NotificationAuditDocument} (Lote 7).
 *
 * <p>Fornece as queries derivadas usadas pelo adapter
 * ({@code existsByEventId}/{@code findByEventId}). E um detalhe de infraestrutura:
 * o handler so conhece a porta {@code NotificationAuditRepository}.
 */
public interface SpringDataNotificationAuditRepository
        extends MongoRepository<NotificationAuditDocument, String> {

    boolean existsByEventId(UUID eventId);

    Optional<NotificationAuditDocument> findByEventId(UUID eventId);
}
