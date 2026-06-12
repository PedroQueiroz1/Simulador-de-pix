package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.domain.ledger.LedgerEntry;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do caso de uso de criacao de Ledger para pagamento aprovado.
 */
@ExtendWith(MockitoExtension.class)
class CreateLedgerForApprovedPaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private IdGenerator idGenerator;

    private CreateLedgerForApprovedPaymentService service() {
        return new CreateLedgerForApprovedPaymentService(ledgerRepository, idGenerator);
    }

    private PixPayment paymentWith(PixPaymentStatus status) {
        return PixPayment.restore(
                PAYMENT_ID, PAYER, RECEIVER, AMOUNT, "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321",
                status, AT, AT,
                status == PixPaymentStatus.APPROVED ? AT : null,
                null);
    }

    @Test
    @DisplayName("Deve criar ledger para pagamento APPROVED")
    void shouldCreateLedgerForApprovedPayment() {
        when(idGenerator.generate()).thenReturn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(ledgerRepository.findByPaymentIdAndOperationType(PAYMENT_ID, LedgerOperationType.PIX_SETTLEMENT))
                .thenReturn(Optional.empty());
        when(ledgerRepository.save(any(LedgerTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        LedgerTransaction ledger = service().createForApprovedPayment(paymentWith(PixPaymentStatus.APPROVED));

        assertEquals(PAYMENT_ID, ledger.getPaymentId());
        assertEquals(LedgerOperationType.PIX_SETTLEMENT, ledger.getOperationType());
        verify(ledgerRepository).save(any(LedgerTransaction.class));
    }

    @Test
    @DisplayName("Nao deve criar ledger para pagamento CREATED")
    void shouldNotCreateLedgerForCreatedPayment() {
        CreateLedgerForApprovedPaymentService service = service();
        PixPayment created = paymentWith(PixPaymentStatus.CREATED);

        assertThrows(IllegalStateException.class, () -> service.createForApprovedPayment(created));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar ledger para pagamento PROCESSING")
    void shouldNotCreateLedgerForProcessingPayment() {
        CreateLedgerForApprovedPaymentService service = service();
        PixPayment processing = paymentWith(PixPaymentStatus.PROCESSING);

        assertThrows(IllegalStateException.class, () -> service.createForApprovedPayment(processing));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar ledger para pagamento REJECTED")
    void shouldNotCreateLedgerForRejectedPayment() {
        CreateLedgerForApprovedPaymentService service = service();
        PixPayment rejected = paymentWith(PixPaymentStatus.REJECTED);

        assertThrows(IllegalStateException.class, () -> service.createForApprovedPayment(rejected));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Se ledger ja existir para paymentId + PIX_SETTLEMENT, nao deve criar outro")
    void shouldNotCreateDuplicateLedger() {
        LedgerTransaction existing = LedgerTransaction.createPixSettlement(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                PAYMENT_ID, PAYER, RECEIVER, AMOUNT, AT);
        when(ledgerRepository.findByPaymentIdAndOperationType(PAYMENT_ID, LedgerOperationType.PIX_SETTLEMENT))
                .thenReturn(Optional.of(existing));

        LedgerTransaction result = service().createForApprovedPayment(paymentWith(PixPaymentStatus.APPROVED));

        assertSame(existing, result);
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar ledger com DEBIT no pagador e CREDIT no recebedor")
    void shouldSaveLedgerWithDebitOnPayerAndCreditOnReceiver() {
        when(idGenerator.generate()).thenReturn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(ledgerRepository.findByPaymentIdAndOperationType(eq(PAYMENT_ID), eq(LedgerOperationType.PIX_SETTLEMENT)))
                .thenReturn(Optional.empty());
        when(ledgerRepository.save(any(LedgerTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service().createForApprovedPayment(paymentWith(PixPaymentStatus.APPROVED));

        ArgumentCaptor<LedgerTransaction> captor = ArgumentCaptor.forClass(LedgerTransaction.class);
        verify(ledgerRepository).save(captor.capture());
        LedgerTransaction saved = captor.getValue();

        LedgerEntry debit = saved.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = saved.getEntries().stream()
                .filter(e -> e.getDirection() == LedgerEntryDirection.CREDIT).findFirst().orElseThrow();

        assertEquals(PAYER, debit.getAccountKey());
        assertEquals(AMOUNT, debit.getAmount());
        assertEquals(RECEIVER, credit.getAccountKey());
        assertEquals(AMOUNT, credit.getAmount());
    }
}
