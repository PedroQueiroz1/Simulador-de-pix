package io.pixsimulator.notification.application.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pixsimulator.notification.application.dto.PaymentEventMessage;
import io.pixsimulator.notification.application.exception.DuplicateEventException;
import io.pixsimulator.notification.application.exception.InvalidPaymentEventException;
import io.pixsimulator.notification.application.notification.NotificationSimulator;
import io.pixsimulator.notification.application.port.out.NotificationAuditRepository;
import io.pixsimulator.notification.domain.NotificationAudit;
import io.pixsimulator.notification.domain.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nucleo de negocio do worker: recebe o payload bruto do Kafka, valida,
 * aplica idempotencia por {@code eventId}, simula a notificacao e grava a
 * auditoria no MongoDB.
 *
 * <p>Fluxo (spec 038):
 * <ol>
 *   <li>parse do JSON e validacao (campos comuns, {@code eventVersion = 1}, tipo
 *       conhecido e campos obrigatorios do tipo);</li>
 *   <li>{@code existsByEventId(eventId)} — se ja processado, ignora com seguranca
 *       (nao simula de novo, nao duplica auditoria);</li>
 *   <li>caso contrario, simula a notificacao e grava auditoria
 *       {@code PROCESSED}.</li>
 * </ol>
 *
 * <p>Tratamento de erro controlado: payload invalido (incluindo versao
 * incompativel e tipo desconhecido) NUNCA derruba o consumer. Se o
 * {@code eventId} for identificavel, grava auditoria {@code FAILED}; senao,
 * apenas loga. A gravacao tambem protege contra corrida no indice unico
 * ({@link DuplicateEventException}).
 *
 * <p>Nao conhece Spring nem MongoDB: depende do {@link ObjectMapper}, do
 * {@link NotificationSimulator} e da porta {@link NotificationAuditRepository},
 * injetados por construtor.
 */
public class PaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

    /** Chaves de MDC para rastreabilidade nos logs do worker. */
    static final String MDC_CORRELATION_ID = "correlationId";
    static final String MDC_EVENT_ID = "eventId";

    /** Unica versao de contrato aceita (spec 036). */
    static final int SUPPORTED_EVENT_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final NotificationSimulator notificationSimulator;
    private final NotificationAuditRepository auditRepository;

    public PaymentEventHandler(ObjectMapper objectMapper,
                               NotificationSimulator notificationSimulator,
                               NotificationAuditRepository auditRepository) {
        this.objectMapper = objectMapper;
        this.notificationSimulator = notificationSimulator;
        this.auditRepository = auditRepository;
    }

    /**
     * Processa uma mensagem Kafka. Qualquer erro e tratado de forma controlada:
     * o metodo nunca propaga excecao para o consumer.
     */
    public void handle(String rawPayload) {
        LocalDateTime receivedAt = LocalDateTime.now();
        UUID eventId = null;
        String correlationId = null;
        try {
            JsonNode node = parseJson(rawPayload);
            eventId = readUuid(node, "eventId");
            // CorrelationId chega do pix-payment-api dentro do payload.
            // Colocado no MDC para ligar os logs do worker a jornada da API.
            correlationId = readText(node, "correlationId");
            putContext(correlationId, eventId);
            log.info("Received payment event {} (correlationId={})", eventId, correlationId);

            PaymentEventMessage event = validate(node, eventId);
            process(event, rawPayload, receivedAt);
        } catch (InvalidPaymentEventException e) {
            handleInvalid(e.getEventId(), correlationId, e.getMessage(), rawPayload, receivedAt);
        } catch (Exception e) {
            // JSON malformado ou erro inesperado: erro controlado, sem derrubar o consumer.
            handleInvalid(eventId, correlationId, "Unexpected error: " + e.getMessage(), rawPayload, receivedAt);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_EVENT_ID);
        }
    }

    private void process(PaymentEventMessage event, String rawPayload, LocalDateTime receivedAt) {
        UUID eventId = event.eventId();

        // Caminho rapido de idempotencia: ja auditado => ignora com seguranca.
        if (auditRepository.existsByEventId(eventId)) {
            log.info("Duplicate payment event {} ({}) ignored; already audited",
                    eventId, event.eventType());
            return;
        }

        String message = notificationSimulator.simulate(event);
        log.info("Simulated notification for payment event {} ({})", eventId, event.eventType());

        NotificationAudit audit = NotificationAudit.processed(
                eventId,
                event.eventType().name(),
                event.eventVersion(),
                event.paymentId(),
                event.correlationId(),
                message,
                rawPayload,
                receivedAt,
                LocalDateTime.now());

        try {
            auditRepository.save(audit);
            log.info("Audit saved for payment event {} ({}) for payment {}",
                    eventId, event.eventType(), event.paymentId());
        } catch (DuplicateEventException e) {
            // Corrida no indice unico: outra entrega gravou primeiro. Ignora com seguranca.
            log.info("Duplicate payment event {} detected on save; ignored", eventId);
        }
    }

    private void handleInvalid(UUID eventId, String correlationId, String errorMessage,
                               String rawPayload, LocalDateTime receivedAt) {
        if (eventId == null) {
            // Sem eventId nao ha como auditar nem deduplicar: apenas loga (spec 038).
            log.warn("Invalid payment event without identifiable eventId, ignored: {}", errorMessage);
            return;
        }

        if (auditRepository.existsByEventId(eventId)) {
            log.info("Invalid payment event {} already audited; ignored", eventId);
            return;
        }

        NotificationAudit audit = NotificationAudit.failed(
                eventId, null, null, null, correlationId, errorMessage, rawPayload, receivedAt, LocalDateTime.now());
        try {
            auditRepository.save(audit);
            log.warn("Recorded FAILED audit for invalid payment event {}: {}", eventId, errorMessage);
        } catch (DuplicateEventException e) {
            log.info("Invalid payment event {} already audited (race); ignored", eventId);
        }
    }

    /** Popula o MDC com correlationId/eventId quando disponiveis. */
    private static void putContext(String correlationId, UUID eventId) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(MDC_CORRELATION_ID, correlationId);
        }
        if (eventId != null) {
            MDC.put(MDC_EVENT_ID, eventId.toString());
        }
    }

    // ------------------------------------------------------------------
    // Parse + validacao
    // ------------------------------------------------------------------

    private JsonNode parseJson(String rawPayload) throws InvalidPaymentEventException {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            // Malformado: sem eventId confiavel => erro controlado, apenas log.
            throw new InvalidPaymentEventException(null, "Malformed JSON payload");
        }
    }

    private PaymentEventMessage validate(JsonNode node, UUID eventId) {
        if (eventId == null) {
            throw new InvalidPaymentEventException(null, "Missing or invalid eventId");
        }

        Integer eventVersion = readInteger(node, "eventVersion");
        if (eventVersion == null || eventVersion != SUPPORTED_EVENT_VERSION) {
            throw new InvalidPaymentEventException(eventId, "Unsupported eventVersion: " + eventVersion);
        }

        String eventTypeRaw = readText(node, "eventType");
        NotificationEventType eventType = NotificationEventType.fromString(eventTypeRaw)
                .orElseThrow(() -> new InvalidPaymentEventException(
                        eventId, "Unknown eventType: " + eventTypeRaw));

        UUID paymentId = readUuid(node, "paymentId");
        String status = readText(node, "status");
        String payerKey = readText(node, "payerKey");
        String receiverKey = readText(node, "receiverKey");
        BigDecimal amount = readDecimal(node, "amount");
        requirePresent(eventId, paymentId, "paymentId");
        requireText(eventId, status, "status");
        requireText(eventId, payerKey, "payerKey");
        requireText(eventId, receiverKey, "receiverKey");
        requirePresent(eventId, amount, "amount");

        LocalDateTime occurredAt = readDateTime(node, "occurredAt");
        String description = null;
        LocalDateTime createdAt = null;
        LocalDateTime processedAt = null;
        UUID ledgerTransactionId = null;
        String rejectionReason = null;

        switch (eventType) {
            case PAYMENT_CREATED -> {
                description = readText(node, "description");
                createdAt = readDateTime(node, "createdAt");
            }
            case PAYMENT_APPROVED -> {
                processedAt = readDateTime(node, "processedAt");
                ledgerTransactionId = readUuid(node, "ledgerTransactionId");
                requirePresent(eventId, ledgerTransactionId, "ledgerTransactionId");
            }
            case PAYMENT_REJECTED -> {
                processedAt = readDateTime(node, "processedAt");
                rejectionReason = readText(node, "rejectionReason");
                requireText(eventId, rejectionReason, "rejectionReason");
            }
        }

        String correlationId = readText(node, "correlationId");

        return new PaymentEventMessage(
                eventId, eventType, eventVersion, occurredAt, paymentId, status,
                payerKey, receiverKey, amount, description, createdAt, processedAt,
                ledgerTransactionId, rejectionReason, correlationId);
    }

    private static void requirePresent(UUID eventId, Object value, String field) {
        if (value == null) {
            throw new InvalidPaymentEventException(eventId, "Missing required field: " + field);
        }
    }

    private static void requireText(UUID eventId, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentEventException(eventId, "Missing required field: " + field);
        }
    }

    private static String readText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static Integer readInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isIntegralNumber()) {
            return value.intValue();
        }
        try {
            return Integer.valueOf(value.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal readDecimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UUID readUuid(JsonNode node, String field) {
        String text = readText(node, field);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(text.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDateTime readDateTime(JsonNode node, String field) {
        String text = readText(node, field);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
