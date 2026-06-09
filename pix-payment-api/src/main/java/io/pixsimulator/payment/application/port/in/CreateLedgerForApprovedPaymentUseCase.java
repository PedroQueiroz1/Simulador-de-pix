package io.pixsimulator.payment.application.port.in;

import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;

/**
 * Porta de entrada (input port) para criar o Ledger de um pagamento aprovado
 * (Lote 5).
 *
 * <p>NAO e exposta por endpoint: o Ledger nasce apenas como efeito do fluxo de
 * processamento aprovado ({@code ProcessPixPaymentUseCase}). Existe como porta
 * para manter a regra de criacao do Ledger isolada e testavel.
 */
public interface CreateLedgerForApprovedPaymentUseCase {

    /**
     * Cria (ou reaproveita, por idempotencia) a {@code LedgerTransaction} de
     * liquidacao ({@code PIX_SETTLEMENT}) de um pagamento aprovado.
     *
     * @param approvedPayment pagamento que acabou de ser aprovado.
     * @return a transacao de ledger criada ou a ja existente.
     */
    LedgerTransaction createForApprovedPayment(PixPayment approvedPayment);
}
