package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Corpo da resposta da consulta de Ledger por pagamento (Lote 5).
 *
 * <pre>
 * {
 *   "paymentId": "...",
 *   "transactions": [
 *     { "ledgerTransactionId": "...", "operationType": "PIX_SETTLEMENT", "createdAt": "...",
 *       "entries": [
 *         { "ledgerEntryId": "...", "direction": "DEBIT",  "accountKey": "...", "amount": 150.75, "createdAt": "..." },
 *         { "ledgerEntryId": "...", "direction": "CREDIT", "accountKey": "...", "amount": 150.75, "createdAt": "..." }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * Pagamento sem ledger devolve {@code transactions} vazia (HTTP 200).
 */
public record LedgerResponse(
        UUID paymentId,
        List<LedgerTransactionResponse> transactions
) {

    public record LedgerTransactionResponse(
            UUID ledgerTransactionId,
            LedgerOperationType operationType,
            LocalDateTime createdAt,
            List<LedgerEntryResponse> entries
    ) {
    }

    public record LedgerEntryResponse(
            UUID ledgerEntryId,
            LedgerEntryDirection direction,
            String accountKey,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
    }

    /** Converte o resultado de aplicacao na resposta HTTP. */
    public static LedgerResponse from(GetLedgerByPaymentResult result) {
        List<LedgerTransactionResponse> transactions = result.transactions().stream()
                .map(LedgerResponse::toTransactionResponse)
                .toList();

        return new LedgerResponse(result.paymentId(), transactions);
    }

    private static LedgerTransactionResponse toTransactionResponse(
            GetLedgerByPaymentResult.LedgerTransactionView view) {

        List<LedgerEntryResponse> entries = view.entries().stream()
                .map(LedgerResponse::toEntryResponse)
                .toList();

        return new LedgerTransactionResponse(
                view.ledgerTransactionId(),
                view.operationType(),
                view.createdAt(),
                entries);
    }

    private static LedgerEntryResponse toEntryResponse(GetLedgerByPaymentResult.LedgerEntryView view) {
        return new LedgerEntryResponse(
                view.ledgerEntryId(),
                view.direction(),
                view.accountKey(),
                view.amount(),
                view.createdAt());
    }
}
