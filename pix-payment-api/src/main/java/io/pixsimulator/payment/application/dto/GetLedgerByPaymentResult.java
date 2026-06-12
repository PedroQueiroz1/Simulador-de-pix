package io.pixsimulator.payment.application.dto;

import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resultado da consulta de Ledger por pagamento.
 *
 * <p>Carrega o {@code paymentId} e a lista de transacoes de ledger (cada uma com
 * suas entries). Quando o pagamento existe mas nao tem ledger, {@code transactions}
 * vem vazia (a API responde 200 com lista vazia).
 */
public record GetLedgerByPaymentResult(
        UUID paymentId,
        List<LedgerTransactionView> transactions
) {

    /** Visao de uma {@code LedgerTransaction} para fora da aplicacao. */
    public record LedgerTransactionView(
            UUID ledgerTransactionId,
            LedgerOperationType operationType,
            LocalDateTime createdAt,
            List<LedgerEntryView> entries
    ) {
    }

    /** Visao de uma {@code LedgerEntry} para fora da aplicacao. */
    public record LedgerEntryView(
            UUID ledgerEntryId,
            LedgerEntryDirection direction,
            String accountKey,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
    }
}
