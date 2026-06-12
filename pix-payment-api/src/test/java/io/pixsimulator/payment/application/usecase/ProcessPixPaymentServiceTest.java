package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.in.CreateLedgerForApprovedPaymentUseCase;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de processamento simulado de pagamento,
 * cobrindo a integracao com o Ledger.
 *
 * <p>A regra e deterministica: {@code amount <= 5000.00} aprova; acima rejeita.
 * Aprovado gera Ledger; rejeitado/terminal nao gera.
 */
@ExtendWith(MockitoExtension.class)
class ProcessPixPaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID LEDGER_TX_ID = UUID.fromString("0197a0e8-7000-7e2f-b3a5-bce754c21abc");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 6, 10, 30, 0);

    @Mock
    private PixPaymentRepository repository;

    @Mock
    private CreateLedgerForApprovedPaymentUseCase createLedgerForApprovedPayment;

    @Mock
    private PaymentOutboxEventService paymentOutboxEventService;

    private ProcessPixPaymentService service() {
        return new ProcessPixPaymentService(repository, createLedgerForApprovedPayment, paymentOutboxEventService);
    }

    /**
     * Stub do caso de uso de Ledger: devolve um settlement (com id fixo
     * {@link #LEDGER_TX_ID}) montado a partir do pagamento aprovado. Usado nos
     * cenarios de aprovacao, onde o servico precisa do {@code ledgerTransactionId}
     * para montar o evento PAYMENT_APPROVED.
     */
    private void stubLedgerCreation() {
        when(createLedgerForApprovedPayment.createForApprovedPayment(any())).thenAnswer(inv -> {
            PixPayment p = inv.getArgument(0);
            return LedgerTransaction.createPixSettlement(
                    LEDGER_TX_ID, UUID.randomUUID(), UUID.randomUUID(),
                    p.getId(), p.getPayerKey(), p.getReceiverKey(), p.getAmount(), CREATED_AT);
        });
    }

    private PixPayment paymentWith(BigDecimal amount, PixPaymentStatus status) {
        return PixPayment.restore(
                PAYMENT_ID,
                "11111111111",
                "22222222222",
                amount,
                "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321",
                status,
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve aprovar pagamento com amount <= 5000.00")
    void shouldApproveWhenAmountUpToLimit() {
        // Limite e inclusivo: exatamente 5000.00 deve aprovar.
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("5000.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubLedgerCreation();

        ProcessPixPaymentResult result = service().process(PAYMENT_ID);

        assertEquals(PixPaymentStatus.APPROVED, result.status());
        assertNotNull(result.processedAt());
        assertNull(result.rejectionReason());
    }

    @Test
    @DisplayName("Deve rejeitar pagamento com amount > 5000.00")
    void shouldRejectWhenAmountAboveLimit() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("5000.01"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPixPaymentResult result = service().process(PAYMENT_ID);

        assertEquals(PixPaymentStatus.REJECTED, result.status());
        assertNotNull(result.processedAt());
        assertEquals("Amount exceeds the simulated approval limit", result.rejectionReason());
    }

    @Test
    @DisplayName("Deve salvar pagamento atualizado")
    void shouldSaveUpdatedPayment() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubLedgerCreation();

        service().process(PAYMENT_ID);

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());
        assertEquals(PixPaymentStatus.APPROVED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getProcessedAt());
    }

    @Test
    @DisplayName("Deve lancar PaymentNotFoundException quando pagamento nao existir")
    void shouldThrowWhenPaymentDoesNotExist() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotFoundException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando pagamento ja estiver APPROVED")
    void shouldThrowWhenAlreadyApproved() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.APPROVED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando pagamento ja estiver REJECTED")
    void shouldThrowWhenAlreadyRejected() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("9000.00"), PixPaymentStatus.REJECTED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));
        verify(repository, never()).save(any(PixPayment.class));
    }

    // ----------------------------------------------------------------------
    // Integracao com o Ledger
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Ao aprovar pagamento, deve criar Ledger")
    void shouldCreateLedgerWhenApproved() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("150.75"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubLedgerCreation();

        service().process(PAYMENT_ID);

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(createLedgerForApprovedPayment).createForApprovedPayment(captor.capture());
        assertEquals(PixPaymentStatus.APPROVED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Ao rejeitar pagamento, nao deve criar Ledger")
    void shouldNotCreateLedgerWhenRejected() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("9000.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().process(PAYMENT_ID);

        verify(createLedgerForApprovedPayment, never()).createForApprovedPayment(any());
    }

    @Test
    @DisplayName("Ao tentar processar pagamento terminal, nao deve criar Ledger")
    void shouldNotCreateLedgerWhenTerminal() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.APPROVED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));
        verify(createLedgerForApprovedPayment, never()).createForApprovedPayment(any());
    }

    // ----------------------------------------------------------------------
    // Integracao com a Outbox
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Pagamento aprovado deve salvar Payment, Ledger e OutboxEvent PAYMENT_APPROVED")
    void shouldSavePaymentLedgerAndApprovedEventWhenApproved() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("150.75"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        stubLedgerCreation();

        service().process(PAYMENT_ID);

        verify(repository).save(any(PixPayment.class));
        verify(createLedgerForApprovedPayment).createForApprovedPayment(any(PixPayment.class));

        // O evento aprovado carrega o ledgerTransactionId do settlement criado.
        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(paymentOutboxEventService).recordPaymentApproved(captor.capture(), eq(LEDGER_TX_ID));
        assertEquals(PixPaymentStatus.APPROVED, captor.getValue().getStatus());
        verify(paymentOutboxEventService, never()).recordPaymentRejected(any());
    }

    @Test
    @DisplayName("Pagamento rejeitado deve salvar Payment e OutboxEvent PAYMENT_REJECTED")
    void shouldSavePaymentAndRejectedEventWhenRejected() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("9000.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().process(PAYMENT_ID);

        verify(repository).save(any(PixPayment.class));
        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(paymentOutboxEventService).recordPaymentRejected(captor.capture());
        assertEquals(PixPaymentStatus.REJECTED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Pagamento rejeitado nao deve criar Ledger nem evento PAYMENT_APPROVED")
    void shouldNotCreateLedgerNorApprovedEventWhenRejected() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("9000.00"), PixPaymentStatus.CREATED)));
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().process(PAYMENT_ID);

        verify(createLedgerForApprovedPayment, never()).createForApprovedPayment(any());
        verify(paymentOutboxEventService, never()).recordPaymentApproved(any(), any());
    }

    @Test
    @DisplayName("Pagamento terminal nao deve criar novo OutboxEvent")
    void shouldNotCreateOutboxEventWhenTerminal() {
        when(repository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(paymentWith(new BigDecimal("100.00"), PixPaymentStatus.APPROVED)));

        ProcessPixPaymentService service = service();
        assertThrows(PaymentNotProcessableException.class, () -> service.process(PAYMENT_ID));

        verify(paymentOutboxEventService, never()).recordPaymentApproved(any(), any());
        verify(paymentOutboxEventService, never()).recordPaymentRejected(any());
    }
}
