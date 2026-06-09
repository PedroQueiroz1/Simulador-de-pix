package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida (output port) para persistencia do Ledger.
 *
 * <p>A aplicacao depende desta interface, nao da tecnologia. A implementacao
 * (Lote 5) e o {@code JpaLedgerRepositoryAdapter} sobre SQL Server.
 *
 * <p>O Ledger e append-only (ADR-023): a porta intencionalmente NAO expoe
 * update nem delete — apenas {@code save} e consultas.
 */
public interface LedgerRepository {

    /** Persiste a transacao de ledger (com suas entries). */
    LedgerTransaction save(LedgerTransaction ledgerTransaction);

    /**
     * Busca a transacao de ledger de um pagamento para um tipo de operacao.
     *
     * <p>Base da idempotencia do Ledger (ADR-024): so pode existir uma
     * {@code LedgerTransaction} por {@code paymentId + operationType}.
     */
    Optional<LedgerTransaction> findByPaymentIdAndOperationType(UUID paymentId,
                                                                LedgerOperationType operationType);

    /** Lista todas as transacoes de ledger de um pagamento (consulta da API). */
    List<LedgerTransaction> findByPaymentId(UUID paymentId);
}
