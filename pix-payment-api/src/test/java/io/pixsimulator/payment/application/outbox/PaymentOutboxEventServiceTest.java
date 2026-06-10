package io.pixsimulator.payment.application.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import io.pixsimulator.payment.observability.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link PaymentOutboxEventService} (Lote 6).
 *
 * <p>Usa um {@link ObjectMapper} real (Jackson + JavaTime) e um
 * {@link IdGenerator} deterministico; o {@link OutboxRepository} e mockado
 * (devolve o evento recebido). Verifica os tres tipos de evento, o mapeamento de
 * aggregate/partitionKey/topic e que o {@code eventId} do payload e igual ao
 * {@code id} do {@link OutboxEvent}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentOutboxEventServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final UUID LEDGER_TX_ID = UUID.fromString("0197a0e8-7000-7e2f-b3a5-bce754c21abc");
    private static final String TOPIC = "pix.payment.events";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 8, 10, 30, 0);
    private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2026, 6, 8, 10, 31, 0);

    @Mock
    private OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Gerador deterministico: todo evento recebe EVENT_ID. */
    private final IdGenerator idGenerator = () -> EVENT_ID;

    private PaymentOutboxEventService service;

    private static final String CORRELATION_ID = "corr-aaaa-bbbb-cccc";

    @BeforeEach
    void setUp() {
        service = new PaymentOutboxEventService(outboxRepository, idGenerator, objectMapper, TOPIC);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private PixPayment createdPayment() {
        return PixPayment.create(PAYMENT_ID, "11111111111", "22222222222", AMOUNT,
                "Pagamento de teste", "idem-key-1");
    }

    private PixPayment terminalPayment(PixPaymentStatus status, String rejectionReason) {
        return PixPayment.restore(PAYMENT_ID, "11111111111", "22222222222", AMOUNT,
                "Pagamento de teste", "idem-key-1", status, CREATED_AT, PROCESSED_AT, PROCESSED_AT, rejectionReason);
    }

    private JsonNode payloadOf(OutboxEvent event) throws Exception {
        return objectMapper.readTree(event.getPayload());
    }

    @Test
    @DisplayName("Deve criar evento PAYMENT_CREATED")
    void shouldCreatePaymentCreatedEvent() throws Exception {
        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        assertEquals("PAYMENT_CREATED", event.getEventType());
        assertEquals("PAYMENT", event.getAggregateType());
        assertEquals(PAYMENT_ID, event.getAggregateId());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());

        JsonNode payload = payloadOf(event);
        assertEquals("PAYMENT_CREATED", payload.get("eventType").asText());
        assertEquals(PAYMENT_ID.toString(), payload.get("paymentId").asText());
        assertEquals(PixPaymentStatus.CREATED.name(), payload.get("status").asText());
        assertEquals("11111111111", payload.get("payerKey").asText());
        assertEquals("22222222222", payload.get("receiverKey").asText());
        assertEquals(0, new BigDecimal(payload.get("amount").asText()).compareTo(AMOUNT));
        assertEquals(1, payload.get("eventVersion").asInt());
    }

    @Test
    @DisplayName("Deve criar evento PAYMENT_APPROVED com ledgerTransactionId")
    void shouldCreatePaymentApprovedEventWithLedgerTransactionId() throws Exception {
        OutboxEvent event = service.recordPaymentApproved(
                terminalPayment(PixPaymentStatus.APPROVED, null), LEDGER_TX_ID);

        assertEquals("PAYMENT_APPROVED", event.getEventType());

        JsonNode payload = payloadOf(event);
        assertEquals("PAYMENT_APPROVED", payload.get("eventType").asText());
        assertEquals(PixPaymentStatus.APPROVED.name(), payload.get("status").asText());
        assertEquals(LEDGER_TX_ID.toString(), payload.get("ledgerTransactionId").asText());
        assertTrue(payload.has("processedAt"));
    }

    @Test
    @DisplayName("Deve criar evento PAYMENT_REJECTED com rejectionReason")
    void shouldCreatePaymentRejectedEventWithReason() throws Exception {
        OutboxEvent event = service.recordPaymentRejected(
                terminalPayment(PixPaymentStatus.REJECTED, "Amount exceeds the simulated approval limit"));

        assertEquals("PAYMENT_REJECTED", event.getEventType());

        JsonNode payload = payloadOf(event);
        assertEquals("PAYMENT_REJECTED", payload.get("eventType").asText());
        assertEquals(PixPaymentStatus.REJECTED.name(), payload.get("status").asText());
        assertEquals("Amount exceeds the simulated approval limit", payload.get("rejectionReason").asText());
    }

    @Test
    @DisplayName("Payload deve conter eventId igual ao OutboxEvent id")
    void payloadEventIdShouldEqualOutboxEventId() throws Exception {
        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        assertEquals(EVENT_ID, event.getId());
        assertEquals(EVENT_ID.toString(), payloadOf(event).get("eventId").asText());
    }

    @Test
    @DisplayName("Deve usar o topic configurado")
    void shouldUseConfiguredTopic() {
        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        assertEquals(TOPIC, event.getTopic());
    }

    @Test
    @DisplayName("Deve usar paymentId como partitionKey")
    void shouldUsePaymentIdAsPartitionKey() {
        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        assertEquals(PAYMENT_ID.toString(), event.getPartitionKey());
    }

    // ----------------------------------------------------------------------
    // Lote 8: propagacao do correlationId (lido do MDC) para o payload.
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("PAYMENT_CREATED deve carregar o correlationId do MDC")
    void createdEventCarriesCorrelationId() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, CORRELATION_ID);

        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        assertEquals(CORRELATION_ID, payloadOf(event).get("correlationId").asText());
    }

    @Test
    @DisplayName("PAYMENT_APPROVED deve carregar o correlationId do MDC")
    void approvedEventCarriesCorrelationId() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, CORRELATION_ID);

        OutboxEvent event = service.recordPaymentApproved(
                terminalPayment(PixPaymentStatus.APPROVED, null), LEDGER_TX_ID);

        assertEquals(CORRELATION_ID, payloadOf(event).get("correlationId").asText());
    }

    @Test
    @DisplayName("PAYMENT_REJECTED deve carregar o correlationId do MDC")
    void rejectedEventCarriesCorrelationId() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, CORRELATION_ID);

        OutboxEvent event = service.recordPaymentRejected(
                terminalPayment(PixPaymentStatus.REJECTED, "Amount exceeds the simulated approval limit"));

        assertEquals(CORRELATION_ID, payloadOf(event).get("correlationId").asText());
    }

    @Test
    @DisplayName("Sem correlationId no MDC, o payload nao deve conter um correlationId nao-nulo")
    void noCorrelationIdWhenMdcEmpty() throws Exception {
        OutboxEvent event = service.recordPaymentCreated(createdPayment());

        // JsonInclude/serializacao mantem o campo, mas com valor nulo (sem vazar lixo).
        assertTrue(payloadOf(event).get("correlationId").isNull());
        assertFalse(payloadOf(event).get("correlationId").asText("").equals("undefined"));
    }
}
