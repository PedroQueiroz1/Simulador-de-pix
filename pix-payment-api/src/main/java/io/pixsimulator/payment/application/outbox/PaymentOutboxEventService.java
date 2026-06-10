package io.pixsimulator.payment.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pixsimulator.payment.application.outbox.payload.PaymentApprovedEventPayload;
import io.pixsimulator.payment.application.outbox.payload.PaymentCreatedEventPayload;
import io.pixsimulator.payment.application.outbox.payload.PaymentRejectedEventPayload;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.observability.MdcKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servico de aplicacao que cria os eventos de pagamento na Outbox (Lote 6).
 *
 * <p>Responsabilidade unica: montar o payload explicito, serializa-lo para JSON
 * com o {@link ObjectMapper}, criar o {@link OutboxEvent} correspondente e
 * salva-lo via {@link OutboxRepository}. E sempre chamado de dentro da transacao
 * do caso de uso (criacao/processamento), de modo que o evento e gravado na
 * MESMA transacao da mudanca de estado (ADR-025).
 *
 * <p>Regras de mapeamento (spec 028):
 * <ul>
 *   <li>{@code aggregateType = PAYMENT}, {@code aggregateId = paymentId},
 *       {@code partitionKey = paymentId.toString()};</li>
 *   <li>o {@code eventId} do payload e o mesmo {@code id} do {@code OutboxEvent}
 *       (gerado pela porta {@link IdGenerator}, UUIDv7);</li>
 *   <li>{@code eventVersion = 1};</li>
 *   <li>todos os eventos vao para o topico configurado
 *       ({@code pix.kafka.topics.payment-events}).</li>
 * </ul>
 *
 * <p>Nao depende de Spring: as dependencias chegam por construtor (montado na
 * borda de configuracao). O dominio permanece sem conhecer Outbox/JSON.
 */
public class PaymentOutboxEventService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxEventService.class);

    /** Tipo de agregado dos eventos de pagamento. */
    static final String AGGREGATE_TYPE_PAYMENT = "PAYMENT";

    /** Versao inicial do contrato de evento (spec 028). */
    static final int EVENT_VERSION = 1;

    private final OutboxRepository outboxRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final String paymentEventsTopic;

    public PaymentOutboxEventService(OutboxRepository outboxRepository,
                                     IdGenerator idGenerator,
                                     ObjectMapper objectMapper,
                                     String paymentEventsTopic) {
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    /** Cria o evento {@code PAYMENT_CREATED} para um pagamento recem-criado. */
    public OutboxEvent recordPaymentCreated(PixPayment payment) {
        UUID eventId = idGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String correlationId = currentCorrelationId();

        PaymentCreatedEventPayload payload = new PaymentCreatedEventPayload(
                eventId,
                PaymentEventType.PAYMENT_CREATED.name(),
                EVENT_VERSION,
                now,
                payment.getId(),
                payment.getStatus().name(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getCreatedAt(),
                correlationId);

        return saveEvent(eventId, payment.getId(), PaymentEventType.PAYMENT_CREATED, payload, now);
    }

    /**
     * Cria o evento {@code PAYMENT_APPROVED}, carregando o
     * {@code ledgerTransactionId} do settlement criado na mesma transacao.
     */
    public OutboxEvent recordPaymentApproved(PixPayment payment, UUID ledgerTransactionId) {
        UUID eventId = idGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String correlationId = currentCorrelationId();

        PaymentApprovedEventPayload payload = new PaymentApprovedEventPayload(
                eventId,
                PaymentEventType.PAYMENT_APPROVED.name(),
                EVENT_VERSION,
                now,
                payment.getId(),
                payment.getStatus().name(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                payment.getProcessedAt(),
                ledgerTransactionId,
                correlationId);

        return saveEvent(eventId, payment.getId(), PaymentEventType.PAYMENT_APPROVED, payload, now);
    }

    /** Cria o evento {@code PAYMENT_REJECTED}, carregando o motivo da recusa. */
    public OutboxEvent recordPaymentRejected(PixPayment payment) {
        UUID eventId = idGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String correlationId = currentCorrelationId();

        PaymentRejectedEventPayload payload = new PaymentRejectedEventPayload(
                eventId,
                PaymentEventType.PAYMENT_REJECTED.name(),
                EVENT_VERSION,
                now,
                payment.getId(),
                payment.getStatus().name(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                payment.getProcessedAt(),
                payment.getRejectionReason(),
                correlationId);

        return saveEvent(eventId, payment.getId(), PaymentEventType.PAYMENT_REJECTED, payload, now);
    }

    private OutboxEvent saveEvent(UUID eventId,
                                  UUID paymentId,
                                  PaymentEventType eventType,
                                  Object payload,
                                  LocalDateTime now) {
        OutboxEvent event = OutboxEvent.create(
                eventId,
                AGGREGATE_TYPE_PAYMENT,
                paymentId,
                eventType.name(),
                EVENT_VERSION,
                paymentEventsTopic,
                paymentId.toString(),
                serialize(payload),
                now);

        OutboxEvent saved = outboxRepository.save(event);
        // Rastreabilidade: liga o OutboxEvent ao pagamento e ao correlationId
        // corrente (que ja segue dentro do payload publicado no Kafka).
        log.info("Created outbox event {} ({}) for payment {}",
                saved.getId(), eventType.name(), paymentId);
        return saved;
    }

    /**
     * Le o correlationId do MDC (definido pelo {@code CorrelationIdFilter} na
     * borda HTTP). Pode ser nulo em fluxos sem requisicao HTTP associada; nesse
     * caso o evento ainda e publicado, apenas sem correlationId.
     */
    private static String currentCorrelationId() {
        return MDC.get(MdcKeys.CORRELATION_ID);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException(
                    "Failed to serialize outbox event payload: " + payload.getClass().getSimpleName(), e);
        }
    }
}
