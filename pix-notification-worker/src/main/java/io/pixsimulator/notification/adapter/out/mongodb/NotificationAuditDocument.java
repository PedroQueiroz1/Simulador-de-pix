package io.pixsimulator.notification.adapter.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Documento de persistencia da auditoria no MongoDB (Lote 7), separado do
 * dominio {@code NotificationAudit}.
 *
 * <p>Mapeia a collection {@code notification_audits}. O {@code eventId} tem
 * indice unico ({@link Indexed}{@code (unique = true)}): e a garantia forte de
 * idempotencia de consumo — uma segunda entrega do mesmo evento viola o indice e
 * a gravacao falha (traduzida em {@code DuplicateEventException} pelo adapter).
 *
 * <p>O indice e criado automaticamente quando
 * {@code spring.data.mongodb.auto-index-creation=true} (ver application.yml).
 */
@Document(collection = "notification_audits")
public class NotificationAuditDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID eventId;

    private String eventType;
    private Integer eventVersion;
    private UUID paymentId;
    private String correlationId;
    private String notificationStatus;
    private String notificationMessage;
    private String rawPayload;
    private String errorMessage;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;

    public NotificationAuditDocument() {
    }

    public NotificationAuditDocument(String id,
                                     UUID eventId,
                                     String eventType,
                                     Integer eventVersion,
                                     UUID paymentId,
                                     String correlationId,
                                     String notificationStatus,
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
