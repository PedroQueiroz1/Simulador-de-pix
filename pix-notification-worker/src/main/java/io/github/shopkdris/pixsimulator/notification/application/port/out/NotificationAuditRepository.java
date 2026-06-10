package io.github.shopkdris.pixsimulator.notification.application.port.out;

import io.github.shopkdris.pixsimulator.notification.domain.NotificationAudit;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para a auditoria de notificacoes (Lote 7).
 *
 * <p>Abstrai o MongoDB do handler. {@link #existsByEventId} e o caminho rapido de
 * idempotencia (antes de simular a notificacao); {@link #save} e a garantia
 * forte, apoiada no indice unico de {@code eventId} — a implementacao deve
 * traduzir a violacao do indice em
 * {@link io.github.shopkdris.pixsimulator.notification.application.exception.DuplicateEventException}.
 */
public interface NotificationAuditRepository {

    /** {@code true} se ja existe auditoria para o {@code eventId} (consumo idempotente). */
    boolean existsByEventId(UUID eventId);

    /**
     * Persiste a auditoria.
     *
     * @throws io.github.shopkdris.pixsimulator.notification.application.exception.DuplicateEventException
     *         se o {@code eventId} ja estiver auditado (violacao do indice unico).
     */
    NotificationAudit save(NotificationAudit audit);

    Optional<NotificationAudit> findByEventId(UUID eventId);
}
