package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;
import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult.LedgerEntryView;
import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult.LedgerTransactionView;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.port.in.GetLedgerByPaymentUseCase;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.ledger.LedgerEntry;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;

import java.util.List;
import java.util.UUID;

/**
 * Consulta o Ledger de um pagamento.
 *
 * <p>Primeiro confirma que o pagamento existe (senao 404 via
 * {@link PaymentNotFoundException}); em seguida le as transacoes de ledger e as
 * converte em {@link GetLedgerByPaymentResult}. Pagamento sem ledger resulta em
 * lista vazia (a API responde 200). Nao depende de Spring: as portas chegam por
 * construtor.
 */
public class GetLedgerByPaymentService implements GetLedgerByPaymentUseCase {

    private final PixPaymentRepository paymentRepository;
    private final LedgerRepository ledgerRepository;

    public GetLedgerByPaymentService(PixPaymentRepository paymentRepository,
                                     LedgerRepository ledgerRepository) {
        this.paymentRepository = paymentRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public GetLedgerByPaymentResult getByPaymentId(UUID paymentId) {
        // O pagamento precisa existir: a ausencia de ledger e valida (200 vazio),
        // mas a ausencia do proprio pagamento e 404.
        paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        List<LedgerTransactionView> transactions = ledgerRepository.findByPaymentId(paymentId).stream()
                .map(GetLedgerByPaymentService::toView)
                .toList();

        return new GetLedgerByPaymentResult(paymentId, transactions);
    }

    private static LedgerTransactionView toView(LedgerTransaction transaction) {
        List<LedgerEntryView> entries = transaction.getEntries().stream()
                .map(GetLedgerByPaymentService::toView)
                .toList();

        return new LedgerTransactionView(
                transaction.getId(),
                transaction.getOperationType(),
                transaction.getCreatedAt(),
                entries);
    }

    private static LedgerEntryView toView(LedgerEntry entry) {
        return new LedgerEntryView(
                entry.getId(),
                entry.getDirection(),
                entry.getAccountKey(),
                entry.getAmount(),
                entry.getCreatedAt());
    }
}
