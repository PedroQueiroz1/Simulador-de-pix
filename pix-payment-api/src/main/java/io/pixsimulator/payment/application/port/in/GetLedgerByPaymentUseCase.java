package io.pixsimulator.payment.application.port.in;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;

import java.util.UUID;

/**
 * Porta de entrada (input port) para consultar o Ledger de um pagamento.
 *
 * <p>Define a intencao "consultar os lancamentos de um pagamento" sem expor HTTP
 * nem persistencia.
 */
public interface GetLedgerByPaymentUseCase {

    /**
     * Retorna as transacoes de ledger do pagamento informado.
     *
     * <p>Se o pagamento existir mas ainda nao tiver ledger (ex.: rejeitado ou
     * nao processado), retorna um resultado com lista vazia. Se o pagamento nao
     * existir, lanca {@code PaymentNotFoundException} (HTTP 404).
     */
    GetLedgerByPaymentResult getByPaymentId(UUID paymentId);
}
