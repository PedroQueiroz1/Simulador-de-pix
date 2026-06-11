package io.pixsimulator.notification.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de auditoria de um evento consumido pelo worker (Lote 7), gravado na
 * collection {@code notification_audits} do MongoDB.
 *
 * <p>Vive no dominio e nao conhece Spring nem MongoDB: o documento de
 * persistencia ({@code NotificationAuditDocument}) e separado e a conversao fica
 * num mapper manual. O {@code eventId} e a chave de idempotencia do consumo
 * (indice unico no MongoDB).
 *
 * <p>Construido por fabricas conforme o desfecho do processamento:
 * {@link #processed} (notificacao simulada com sucesso) ou {@link #failed}
 * (payload invalido/erro controlado, com {@code eventId} identificavel). O
 * {@link #restore} reconstroi a partir do documento persistido, sem reaplicar
 * regras.
 */
public final class NotificationAudit {

    /** Limite de tamanho da mensagem de erro persistida (nunca a stack trace). */
    static final int MAX_ERROR_LENGTH = 500;

    private final String id;
    private final UUID eventId;
    private final String eventType;
    private final Integer eventVersion;
    private final UUID paymentId;
    private final String correlationId;
    private final NotificationStatus notificationStatus;
    private final String notificationMessage;
    private final String rawPayload;
    private final String errorMessage;
    private final LocalDateTime receivedAt;
    private final LocalDateTime processedAt;

    private NotificationAudit(String id,
                              UUID eventId,
                              String eventType,
                              Integer eventVersion,
                              UUID paymentId,
                              String correlationId,
                              NotificationStatus notificationStatus,
                              String notificationMessage,
                              String rawPayload,
                              String errorMessage,
                              LocalDateTime receivedAt,
                              LocalDateTime processedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.paymentId = paymentId;
        this.correlationId = correlationId;
        this.notificationStatus = notificationStatus;
        this.notificationMessage = notificationMessage;
        this.rawPayload = rawPayload;
        this.errorMessage = errorMessage;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    /**
     * Auditoria de sucesso ({@link NotificationStatus#PROCESSED}): evento valido,
     * notificacao simulada. {@code id} fica nulo (o MongoDB gera ao salvar) e
     * {@code errorMessage} permanece nulo.
     */
    public static NotificationAudit processed(UUID eventId,
                                              String eventType,
                                              Integer eventVersion,
                                              UUID paymentId,
                                              String correlationId,
                                              String notificationMessage,
                                              String rawPayload,
                                              LocalDateTime receivedAt,
                                              LocalDateTime processedAt) {
        return new NotificationAudit(
                null, eventId, eventType, eventVersion, paymentId, correlationId,
                NotificationStatus.PROCESSED, notificationMessage, rawPayload,
                null, receivedAt, processedAt);
    }

    /**
     * Auditoria de falha ({@link NotificationStatus#FAILED}): payload invalido ou
     * erro controlado, com {@code eventId} identificavel. Guarda apenas uma
     * mensagem de erro curta (truncada), nunca a stack trace; campos que nao
     * puderam ser extraidos do payload podem chegar nulos.
     */
    public static NotificationAudit failed(UUID eventId,
                                           String eventType,
                                           Integer eventVersion,
                                           UUID paymentId,
                                           String correlationId,
                                           String errorMessage,
                                           String rawPayload,
                                           LocalDateTime receivedAt,
                                           LocalDateTime processedAt) {
        return new NotificationAudit(
                null, eventId, eventType, eventVersion, paymentId, correlationId,
                NotificationStatus.FAILED, null, rawPayload,
                truncateError(errorMessage), receivedAt, processedAt);
    }

    /**
     * Reconstroi uma auditoria ja persistida a partir de todos os seus campos
     * (usado pelo mapper Mongo, documento -&gt; dominio). Preserva fielmente o
     * estado salvo, sem truncar nem reaplicar regras.
     */
    public static NotificationAudit restore(String id,
                                            UUID eventId,
                                            String eventType,
                                            Integer eventVersion,
                                            UUID paymentId,
                                            String correlationId,
                                            NotificationStatus notificationStatus,
                                            String notificationMessage,
                                            String rawPayload,
                                            String errorMessage,
                                            LocalDateTime receivedAt,
                                            LocalDateTime processedAt) {
        return new NotificationAudit(
                id, eventId, eventType, eventVersion, paymentId, correlationId, notificationStatus,
                notificationMessage, rawPayload, errorMessage, receivedAt, processedAt);
    }

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    public String getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationAudit)) {
            return false;
        }
        NotificationAudit that = (NotificationAudit) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
