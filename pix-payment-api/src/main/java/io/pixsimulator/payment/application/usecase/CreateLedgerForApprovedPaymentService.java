package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.port.in.CreateLedgerForApprovedPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Cria o Ledger ({@code PIX_SETTLEMENT}) de um pagamento aprovado (Lote 5).
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>so cria ledger para pagamento {@link PixPaymentStatus#APPROVED} — um
 *       pagamento em outro status nunca deveria chegar aqui (invariante de
 *       fluxo), entao falha com {@link IllegalStateException};</li>
 *   <li>idempotencia propria (ADR-024): se ja existir ledger para
 *       {@code paymentId + PIX_SETTLEMENT}, devolve o existente e nao cria outro;</li>
 *   <li>caso contrario, monta a {@link LedgerTransaction} de liquidacao (DEBIT
 *       do pagador, CREDIT do recebedor) e salva.</li>
 * </ol>
 *
 * <p>A atomicidade com a aprovacao do pagamento e garantida pela transacao
 * aberta no {@code ProcessPixPaymentService}; este servico apenas participa
 * dela. Nao depende de Spring: as portas chegam por construtor.
 */
public class CreateLedgerForApprovedPaymentService implements CreateLedgerForApprovedPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateLedgerForApprovedPaymentService.class);

    private final LedgerRepository ledgerRepository;
    private final IdGenerator idGenerator;

    public CreateLedgerForApprovedPaymentService(LedgerRepository ledgerRepository,
                                                 IdGenerator idGenerator) {
        this.ledgerRepository = ledgerRepository;
        this.idGenerator = idGenerator;
    }

    @Override
    public LedgerTransaction createForApprovedPayment(PixPayment approvedPayment) {
        if (approvedPayment.getStatus() != PixPaymentStatus.APPROVED) {
            // Guard de invariante: Ledger so existe para pagamento aprovado.
            throw new IllegalStateException(
                    "Ledger can only be created for an APPROVED payment (status="
                            + approvedPayment.getStatus() + ")");
        }

        // Idempotencia do Ledger: nao duplicar o settlement de um mesmo pagamento.
        return ledgerRepository
                .findByPaymentIdAndOperationType(approvedPayment.getId(), LedgerOperationType.PIX_SETTLEMENT)
                .orElseGet(() -> {
                    LedgerTransaction ledger = ledgerRepository.save(buildSettlement(approvedPayment));
                    log.info("Created ledger transaction {} (PIX_SETTLEMENT) for payment {}",
                            ledger.getId(), approvedPayment.getId());
                    return ledger;
                });
    }

    private LedgerTransaction buildSettlement(PixPayment payment) {
        return LedgerTransaction.createPixSettlement(
                idGenerator.generate(),   // ledgerTransactionId
                idGenerator.generate(),   // debitEntryId
                idGenerator.generate(),   // creditEntryId
                payment.getId(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                LocalDateTime.now());
    }
}
