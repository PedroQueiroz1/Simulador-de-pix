package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA da tabela {@code ledger_entries}.
 *
 * <p>Detalhe de infraestrutura, separado do dominio
 * {@code domain.ledger.LedgerEntry}: so aqui vivem as anotacoes JPA. A
 * {@code direction} e persistida como {@code String} (nome do enum), nunca como
 * ordinal.
 *
 * <p>A coluna {@code ledger_transaction_id} e mapeada como atributo proprio
 * (escrita): e ela que liga a entry a sua transacao no INSERT. A
 * {@code LedgerTransactionEntity} apenas a referencia em modo leitura no seu
 * {@code @OneToMany}, evitando duplo gerenciamento da mesma coluna.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "account_key", nullable = false, length = 120)
    private String accountKey;

    @Column(name = "direction", nullable = false, length = 20)
    private String direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Construtor sem argumentos exigido pelo JPA/Hibernate. */
    protected LedgerEntryEntity() {
    }

    public LedgerEntryEntity(UUID id,
                             UUID ledgerTransactionId,
                             UUID paymentId,
                             String accountKey,
                             String direction,
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

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
