package io.pixsimulator.payment.domain.ledger;

/**
 * Sentido de um lancamento contabil ({@link LedgerEntry}).
 *
 * <p>O Ledger usa modelo de dupla entrada simplificado (ADR-022): o valor das
 * entries e sempre positivo e e a direcao que define o sentido do movimento.
 *
 * <ul>
 *   <li>{@link #DEBIT} — saida (no PIX_SETTLEMENT, o pagador);</li>
 *   <li>{@link #CREDIT} — entrada (no PIX_SETTLEMENT, o recebedor).</li>
 * </ul>
 */
public enum LedgerEntryDirection {
    DEBIT,
    CREDIT
}
