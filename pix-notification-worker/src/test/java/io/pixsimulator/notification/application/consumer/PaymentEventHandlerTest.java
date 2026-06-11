package io.pixsimulator.notification.application.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pixsimulator.notification.application.dto.PaymentEventMessage;
import io.pixsimulator.notification.application.notification.NotificationSimulator;
import io.pixsimulator.notification.application.port.out.NotificationAuditRepository;
import io.pixsimulator.notification.domain.NotificationAudit;
import io.pixsimulator.notification.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link PaymentEventHandler} com {@link NotificationSimulator} e
 * {@link NotificationAuditRepository} mockados (Lote 7).
 *
 * <p>Cobre o caminho feliz dos tres tipos de evento, a idempotencia por
 * {@code eventId} (duplicado nao simula nem salva de novo) e o tratamento
 * controlado de payloads invalidos: malformado (apenas log), campo obrigatorio
 * ausente, tipo desconhecido e versao incompativel — nenhum deles derruba o
 * handler.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerTest {

    private static final UUID EVENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final UUID LEDGER_TX_ID = UUID.fromString("0197b111-2222-7333-8444-555566667777");
    private static final String CORRELATION_ID = "11111111-2222-3333-4444-555555555555";

    @Mock
    private NotificationSimulator notificationSimulator;

    @Mock
    private NotificationAuditRepository auditRepository;

    private PaymentEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentEventHandler(new ObjectMapper(), notificationSimulator, auditRepository);
    }

    private String paymentCreatedJson() {
        return """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_CREATED",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-09T10:30:00",
                  "paymentId": "%s",
                  "status": "CREATED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75,
                  "description": "Pagamento de teste",
                  "createdAt": "2026-06-09T10:30:00"
                }
                """.formatted(EVENT_ID, PAYMENT_ID);
    }

    private String paymentApprovedJson(String ledgerTransactionIdField) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_APPROVED",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-09T10:31:00",
                  "paymentId": "%s",
                  "status": "APPROVED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75,
                  "processedAt": "2026-06-09T10:31:00"%s
                }
                """.formatted(EVENT_ID, PAYMENT_ID, ledgerTransactionIdField);
    }

    private String paymentRejectedJson(String rejectionReasonField) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_REJECTED",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-09T10:32:00",
                  "paymentId": "%s",
                  "status": "REJECTED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75,
                  "processedAt": "2026-06-09T10:32:00"%s
                }
                """.formatted(EVENT_ID, PAYMENT_ID, rejectionReasonField);
    }

    private NotificationAudit captureSavedAudit() {
        ArgumentCaptor<NotificationAudit> captor = ArgumentCaptor.forClass(NotificationAudit.class);
        verify(auditRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Deve processar PAYMENT_CREATED valido: simula notificacao e salva auditoria PROCESSED")
    void processesValidPaymentCreated() {
        when(notificationSimulator.simulate(any(PaymentEventMessage.class))).thenReturn("Notificacao simulada");

        handler.handle(paymentCreatedJson());

        verify(auditRepository).existsByEventId(EVENT_ID);
        verify(notificationSimulator).simulate(any(PaymentEventMessage.class));
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.PROCESSED, saved.getNotificationStatus());
        assertEquals(EVENT_ID, saved.getEventId());
        assertEquals(PAYMENT_ID, saved.getPaymentId());
        assertEquals("PAYMENT_CREATED", saved.getEventType());
        assertEquals(1, saved.getEventVersion());
        assertEquals("Notificacao simulada", saved.getNotificationMessage());
        assertNotNull(saved.getRawPayload());
    }

    @Test
    @DisplayName("Lote 8: deve gravar o correlationId do payload na auditoria")
    void savesCorrelationIdFromPayload() {
        when(notificationSimulator.simulate(any(PaymentEventMessage.class))).thenReturn("Notificacao simulada");

        String json = """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_CREATED",
                  "eventVersion": 1,
                  "occurredAt": "2026-06-09T10:30:00",
                  "paymentId": "%s",
                  "status": "CREATED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75,
                  "description": "Pagamento de teste",
                  "createdAt": "2026-06-09T10:30:00",
                  "correlationId": "%s"
                }
                """.formatted(EVENT_ID, PAYMENT_ID, CORRELATION_ID);

        handler.handle(json);

        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.PROCESSED, saved.getNotificationStatus());
        assertEquals(CORRELATION_ID, saved.getCorrelationId());
    }

    @Test
    @DisplayName("Deve processar PAYMENT_APPROVED valido (com ledgerTransactionId): salva auditoria PROCESSED")
    void processesValidPaymentApproved() {
        when(notificationSimulator.simulate(any(PaymentEventMessage.class))).thenReturn("Notificacao simulada");

        handler.handle(paymentApprovedJson(",\n  \"ledgerTransactionId\": \"" + LEDGER_TX_ID + "\""));

        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.PROCESSED, saved.getNotificationStatus());
        assertEquals("PAYMENT_APPROVED", saved.getEventType());
    }

    @Test
    @DisplayName("Deve processar PAYMENT_REJECTED valido (com rejectionReason): salva auditoria PROCESSED")
    void processesValidPaymentRejected() {
        when(notificationSimulator.simulate(any(PaymentEventMessage.class))).thenReturn("Notificacao simulada");

        handler.handle(paymentRejectedJson(",\n  \"rejectionReason\": \"Saldo insuficiente\""));

        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.PROCESSED, saved.getNotificationStatus());
        assertEquals("PAYMENT_REJECTED", saved.getEventType());
    }

    @Test
    @DisplayName("Deve ignorar com seguranca um evento duplicado por eventId (nao salva nova auditoria)")
    void ignoresDuplicateEventById() {
        when(auditRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        handler.handle(paymentCreatedJson());

        verify(auditRepository).existsByEventId(EVENT_ID);
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Evento duplicado nao deve simular notificacao novamente")
    void duplicateEventDoesNotSimulateAgain() {
        when(auditRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        handler.handle(paymentCreatedJson());

        verify(notificationSimulator, never()).simulate(any());
    }

    @Test
    @DisplayName("Payload malformado (sem eventId) nao deve derrubar o handler nem salvar auditoria")
    void malformedPayloadDoesNotCrashHandler() {
        handler.handle("{ isto nao e json valido ");

        verify(notificationSimulator, never()).simulate(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payload invalido com eventId (campo obrigatorio ausente) deve salvar auditoria FAILED")
    void invalidPayloadWithEventIdSavesFailedAudit() {
        // Sem receiverKey: campo obrigatorio ausente, mas eventId e identificavel.
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_CREATED",
                  "eventVersion": 1,
                  "paymentId": "%s",
                  "status": "CREATED",
                  "payerKey": "11111111111",
                  "amount": 150.75
                }
                """.formatted(EVENT_ID, PAYMENT_ID);

        handler.handle(json);

        verify(notificationSimulator, never()).simulate(any());
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.FAILED, saved.getNotificationStatus());
        assertEquals(EVENT_ID, saved.getEventId());
        assertNotNull(saved.getErrorMessage());
    }

    @Test
    @DisplayName("Tipo de evento desconhecido deve ser erro controlado: auditoria FAILED, sem derrubar o handler")
    void unknownEventTypeIsControlledError() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_CANCELLED",
                  "eventVersion": 1,
                  "paymentId": "%s",
                  "status": "CANCELLED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75
                }
                """.formatted(EVENT_ID, PAYMENT_ID);

        handler.handle(json);

        verify(notificationSimulator, never()).simulate(any());
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.FAILED, saved.getNotificationStatus());
        assertEquals(EVENT_ID, saved.getEventId());
    }

    @Test
    @DisplayName("Versao incompativel deve ser erro controlado: auditoria FAILED, sem derrubar o handler")
    void incompatibleVersionIsControlledError() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "PAYMENT_CREATED",
                  "eventVersion": 2,
                  "paymentId": "%s",
                  "status": "CREATED",
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 150.75
                }
                """.formatted(EVENT_ID, PAYMENT_ID);

        handler.handle(json);

        verify(notificationSimulator, never()).simulate(any());
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.FAILED, saved.getNotificationStatus());
        assertEquals(EVENT_ID, saved.getEventId());
    }

    @Test
    @DisplayName("PAYMENT_APPROVED sem ledgerTransactionId (obrigatorio) deve ser erro controlado FAILED")
    void approvedWithoutLedgerTransactionIdIsControlledError() {
        handler.handle(paymentApprovedJson(""));

        verify(notificationSimulator, never()).simulate(any());
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.FAILED, saved.getNotificationStatus());
    }

    @Test
    @DisplayName("PAYMENT_REJECTED sem rejectionReason (obrigatorio) deve ser erro controlado FAILED")
    void rejectedWithoutReasonIsControlledError() {
        handler.handle(paymentRejectedJson(""));

        verify(notificationSimulator, never()).simulate(any());
        NotificationAudit saved = captureSavedAudit();
        assertEquals(NotificationStatus.FAILED, saved.getNotificationStatus());
    }
}
