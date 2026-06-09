package io.pixsimulator.payment.domain.ledger;

import io.pixsimulator.payment.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Linha de um lancamento contabil do Ledger.
 *
 * <p>Cada entry tem uma {@link LedgerEntryDirection} (DEBIT ou CREDIT) e um
 * {@code amount} sempre <strong>positivo</strong> (ADR-022): nunca se usa valor
 * negativo; e a direcao que define o sentido do movimento.
 *
 * <p>O Ledger e append-only (ADR-023): a entry e imutavel. Todos os campos sao
 * {@code final} e nao ha setters. A criacao passa pela fabrica {@link #create},
 * que valida as invariantes; {@link #restore} apenas reconstroi a partir de
 * dados ja persistidos (caminho de leitura do mapper).
 *
 * <p>Dominio puro: sem anotacoes JPA e sem dependencia de Spring.
 */
public final class LedgerEntry {

    private final UUID id;
    private final UUID ledgerTransactionId;
    private final UUID paymentId;
    private final String accountKey;
    private final LedgerEntryDirection direction;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;

    private LedgerEntry(UUID id,
                        UUID ledgerTransactionId,
                        UUID paymentId,
                        String accountKey,
                        LedgerEntryDirection direction,
                        BigDecimal amount,
                        LocalDateTime createdAt) {
        this.id = id;
        this.ledgerTransactionId = ledgerTransactionId;
        this.paymentId = paymentId;
        this.accountKey = accountKey;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    /**
     * Cria uma entry valida.
     *
     * <p>Valida as invariantes de uma linha contabil: identificadores e chave
     * presentes, direcao definida e, principalmente, {@code amount} estritamente
     * positivo (zero ou negativo violam a regra do Ledger).
     *
     * @throws DomainException se alguma invariante for violada.
     */
    public static LedgerEntry create(UUID id,
                                     UUID ledgerTransactionId,
                                     UUID paymentId,
                                     String accountKey,
                                     LedgerEntryDirection direction,
                                     BigDecimal amount,
                                     LocalDateTime createdAt) {
        if (id == null) {
            throw new DomainException("ledger entry id is required");
        }
        if (ledgerTransactionId == null) {
            throw new DomainException("ledgerTransactionId is required");
        }
        if (paymentId == null) {
            throw new DomainException("paymentId is required");
        }
        if (accountKey == null || accountKey.isBlank()) {
            throw new DomainException("accountKey is required");
        }
        if (direction == null) {
            throw new DomainException("direction is required");
        }
        if (amount == null) {
            throw new DomainException("amount is required");
        }
        // Regra do Ledger: amount sempre positivo; o sentido vem da direction.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("ledger entry amount must be positive (was " + amount + ")");
        }
        if (createdAt == null) {
            throw new DomainException("createdAt is required");
        }
        return new LedgerEntry(id, ledgerTransactionId, paymentId, accountKey, direction, amount, createdAt);
    }

    /**
     * Reconstroi uma entry ja persistida, sem reaplicar regras de criacao.
     *
     * <p>Usado pelo mapper de persistencia (Entity -&gt; Domain). Os dados ja
     * foram validados quando a entry foi criada; aqui apenas se preserva o
     * estado salvo.
     */
    public static LedgerEntry restore(UUID id,
                                      UUID ledgerTransactionId,
                                      UUID paymentId,
                                      String accountKey,
                                      LedgerEntryDirection direction,
                                      BigDecimal amount,
                                      LocalDateTime createdAt) {
        return new LedgerEntry(id, ledgerTransactionId, paymentId, accountKey, direction, amount, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public LedgerEntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LedgerEntry)) {
            return false;
        }
        LedgerEntry that = (LedgerEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
