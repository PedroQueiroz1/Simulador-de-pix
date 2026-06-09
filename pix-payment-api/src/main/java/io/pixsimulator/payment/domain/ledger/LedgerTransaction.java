package io.pixsimulator.payment.domain.ledger;

import io.pixsimulator.payment.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Conjunto de lancamentos contabeis ({@link LedgerEntry}) de uma operacao.
 *
 * <p>Representa os efeitos financeiros de um pagamento aprovado (ADR-020:
 * Payment = intencao/ciclo de vida; Ledger = efeitos financeiros). Para
 * {@link LedgerOperationType#PIX_SETTLEMENT} gera exatamente 1 DEBIT do pagador
 * e 1 CREDIT do recebedor.
 *
 * <p>Invariantes garantidas na construcao:
 * <ul>
 *   <li>pelo menos 1 DEBIT e 1 CREDIT;</li>
 *   <li>fechamento contabil em zero: {@code totalDebits == totalCredits}
 *       (ADR-022). Se nao fechar, lanca {@link DomainException}.</li>
 * </ul>
 *
 * <p>Append-only (ADR-023): a transacao e imutavel. A lista retornada por
 * {@link #getEntries()} e nao modificavel e nao ha metodos para alterar ou
 * remover entries. Correcoes futuras devem ser lancamentos compensatorios.
 *
 * <p>Dominio puro: sem anotacoes JPA e sem dependencia de Spring.
 */
public final class LedgerTransaction {

    private final UUID id;
    private final UUID paymentId;
    private final LedgerOperationType operationType;
    private final List<LedgerEntry> entries;
    private final LocalDateTime createdAt;

    private LedgerTransaction(UUID id,
                              UUID paymentId,
                              LedgerOperationType operationType,
                              List<LedgerEntry> entries,
                              LocalDateTime createdAt) {
        if (id == null) {
            throw new DomainException("ledger transaction id is required");
        }
        if (paymentId == null) {
            throw new DomainException("paymentId is required");
        }
        if (operationType == null) {
            throw new DomainException("operationType is required");
        }
        if (entries == null || entries.isEmpty()) {
            throw new DomainException("a ledger transaction must have entries");
        }
        if (createdAt == null) {
            throw new DomainException("createdAt is required");
        }
        // List.copyOf devolve uma lista imutavel: garante o append-only no dominio.
        this.id = id;
        this.paymentId = paymentId;
        this.operationType = operationType;
        this.entries = List.copyOf(entries);
        this.createdAt = createdAt;
        validateAccountingClosure();
    }

    /**
     * Cria a liquidacao contabil de um pagamento Pix aprovado.
     *
     * <p>Gera duas entries equilibradas:
     * <pre>
     *   DEBIT  payerKey    amount
     *   CREDIT receiverKey amount
     * </pre>
     * e valida o fechamento em zero. O {@code amount} deve ser positivo (a
     * validacao por entry barra zero/negativo).
     *
     * @throws DomainException se o amount nao for positivo ou o fechamento falhar.
     */
    public static LedgerTransaction createPixSettlement(UUID ledgerTransactionId,
                                                        UUID debitEntryId,
                                                        UUID creditEntryId,
                                                        UUID paymentId,
                                                        String payerKey,
                                                        String receiverKey,
                                                        BigDecimal amount,
                                                        LocalDateTime now) {

        LedgerEntry debit = LedgerEntry.create(
                debitEntryId, ledgerTransactionId, paymentId, payerKey,
                LedgerEntryDirection.DEBIT, amount, now);

        LedgerEntry credit = LedgerEntry.create(
                creditEntryId, ledgerTransactionId, paymentId, receiverKey,
                LedgerEntryDirection.CREDIT, amount, now);

        return new LedgerTransaction(
                ledgerTransactionId,
                paymentId,
                LedgerOperationType.PIX_SETTLEMENT,
                List.of(debit, credit),
                now);
    }

    /**
     * Reconstroi uma transacao ja persistida a partir de suas entries.
     *
     * <p>Usado pelo mapper de persistencia (Entity -&gt; Domain). As invariantes
     * de fechamento sao revalidadas: um ledger sempre deve fechar em zero, mesmo
     * vindo do banco.
     */
    public static LedgerTransaction restore(UUID id,
                                            UUID paymentId,
                                            LedgerOperationType operationType,
                                            List<LedgerEntry> entries,
                                            LocalDateTime createdAt) {
        return new LedgerTransaction(id, paymentId, operationType, entries, createdAt);
    }

    /**
     * Garante a regra contabil: pelo menos 1 DEBIT, pelo menos 1 CREDIT e
     * total de debitos igual ao total de creditos. Caso contrario,
     * {@link DomainException}.
     */
    private void validateAccountingClosure() {
        boolean hasDebit = false;
        boolean hasCredit = false;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (entry.getDirection() == LedgerEntryDirection.DEBIT) {
                hasDebit = true;
                totalDebits = totalDebits.add(entry.getAmount());
            } else if (entry.getDirection() == LedgerEntryDirection.CREDIT) {
                hasCredit = true;
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (!hasDebit) {
            throw new DomainException("ledger transaction must have at least one DEBIT entry");
        }
        if (!hasCredit) {
            throw new DomainException("ledger transaction must have at least one CREDIT entry");
        }
        // compareTo (e nao equals) para ignorar diferencas de escala (ex.: 10 vs 10.00).
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new DomainException(
                    "ledger does not balance: total debits (" + totalDebits
                            + ") must equal total credits (" + totalCredits + ")");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public LedgerOperationType getOperationType() {
        return operationType;
    }

    /** Lista imutavel de entries (append-only: nao pode ser alterada). */
    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerTransaction)) {
            return false;
        }
        LedgerTransaction that = (LedgerTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
