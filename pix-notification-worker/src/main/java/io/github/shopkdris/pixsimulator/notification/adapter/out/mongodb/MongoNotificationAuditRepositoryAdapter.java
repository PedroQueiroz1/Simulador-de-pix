package io.github.shopkdris.pixsimulator.notification.adapter.out.mongodb;

import io.github.shopkdris.pixsimulator.notification.application.exception.DuplicateEventException;
import io.github.shopkdris.pixsimulator.notification.application.port.out.NotificationAuditRepository;
import io.github.shopkdris.pixsimulator.notification.domain.NotificationAudit;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saida MongoDB para a auditoria de notificacoes (Lote 7).
 *
 * <p>Implementa a porta {@link NotificationAuditRepository} delegando ao
 * {@link SpringDataNotificationAuditRepository} e convertendo dominio &harr;
 * documento pelo {@link NotificationAuditMongoMapper}.
 *
 * <p>Traduz a violacao do indice unico de {@code eventId}
 * ({@link DuplicateKeyException}) em {@link DuplicateEventException}, de forma que
 * o handler trate o duplicado de maneira controlada sem conhecer detalhes do
 * Spring Data.
 */
@Component
public class MongoNotificationAuditRepositoryAdapter implements NotificationAuditRepository {

    private final SpringDataNotificationAuditRepository repository;

    public MongoNotificationAuditRepositoryAdapter(SpringDataNotificationAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public NotificationAudit save(NotificationAudit audit) {
        try {
            NotificationAuditDocument saved =
                    repository.save(NotificationAuditMongoMapper.toDocument(audit));
            return NotificationAuditMongoMapper.toDomain(saved);
        } catch (DuplicateKeyException e) {
            throw new DuplicateEventException(audit.getEventId(), e);
        }
    }

    @Override
    public Optional<NotificationAudit> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).map(NotificationAuditMongoMapper::toDomain);
    }
}
