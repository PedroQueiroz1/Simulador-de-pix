package io.pixsimulator.notification.adapter.out.mongodb;

import io.pixsimulator.notification.domain.NotificationAudit;
import io.pixsimulator.notification.domain.NotificationStatus;

/**
 * Mapper manual entre o dominio {@link NotificationAudit} e o documento Mongo
 * {@link NotificationAuditDocument} (Lote 7).
 *
 * <p>Mantem o dominio livre de anotacoes do Spring Data. A traducao do
 * {@link NotificationStatus} e feita por nome ({@code enum <-> String}),
 * preservando {@code eventId}, {@code eventType}, {@code paymentId},
 * {@code rawPayload} e {@code notificationStatus} nos dois sentidos.
 */
public final class NotificationAuditMongoMapper {

    private NotificationAuditMongoMapper() {
    }

    public static NotificationAuditDocument toDocument(NotificationAudit audit) {
        return new NotificationAuditDocument(
                audit.getId(),
                audit.getEventId(),
                audit.getEventType(),
                audit.getEventVersion(),
                audit.getPaymentId(),
                audit.getCorrelationId(),
                audit.getNotificationStatus() != null ? audit.getNotificationStatus().name() : null,
                audit.getNotificationMessage(),
                audit.getRawPayload(),
                audit.getErrorMessage(),
                audit.getReceivedAt(),
                audit.getProcessedAt());
    }

    public static NotificationAudit toDomain(NotificationAuditDocument document) {
        return NotificationAudit.restore(
                document.getId(),
                document.getEventId(),
                document.getEventType(),
                document.getEventVersion(),
                document.getPaymentId(),
                document.getCorrelationId(),
                document.getNotificationStatus() != null
                        ? NotificationStatus.valueOf(document.getNotificationStatus())
                        : null,
                document.getNotificationMessage(),
                document.getRawPayload(),
                document.getErrorMessage(),
                document.getReceivedAt(),
                document.getProcessedAt());
    }
}
