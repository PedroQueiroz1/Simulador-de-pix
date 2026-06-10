package io.github.shopkdris.pixsimulator.notification.domain;

/**
 * Resultado do processamento de um evento pelo worker (Lote 7), gravado em
 * {@code notification_audits.notificationStatus}.
 *
 * <ul>
 *   <li>{@link #PROCESSED}: evento valido, notificacao simulada com sucesso;</li>
 *   <li>{@link #FAILED}: payload invalido/erro controlado, com {@code eventId}
 *       identificavel (a auditoria registra o motivo curto);</li>
 *   <li>{@link #IGNORED}: evento descartado de forma segura (reservado para
 *       cenarios de descarte explicito).</li>
 * </ul>
 */
public enum NotificationStatus {
    PROCESSED,
    FAILED,
    IGNORED
}
