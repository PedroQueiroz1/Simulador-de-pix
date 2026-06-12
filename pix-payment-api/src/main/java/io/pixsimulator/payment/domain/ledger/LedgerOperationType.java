package io.pixsimulator.payment.domain.ledger;

/**
 * Tipo de operacao que origina uma {@link LedgerTransaction}.
 *
 * <p>Existe apenas {@link #PIX_SETTLEMENT}: a liquidacao de um
 * pagamento Pix aprovado (1 DEBIT do pagador + 1 CREDIT do recebedor).
 *
 * <p>Reversoes/estornos futuros devem usar um novo {@code operationType}
 * (ADR-024), nunca editar ou apagar o settlement existente.
 */
public enum LedgerOperationType {
    PIX_SETTLEMENT
}
