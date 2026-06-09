package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA da tabela {@code ledger_transactions}.
 *
 * <p>Detalhe de infraestrutura, separado do dominio
 * {@code domain.ledger.LedgerTransaction}. A {@code operation_type} e persistida
 * como {@code String} (nome do enum). A constraint unica
 * {@code (payment_id, operation_type)} reflete a idempotencia do Ledger (ADR-024).
 *
 * <p>As entries sao gravadas em cascata ({@code CascadeType.ALL},
 * {@code orphanRemoval = false} — Ledger e append-only, ADR-023). O
 * {@code @JoinColumn} e marcado como somente leitura
 * ({@code insertable/updatable = false}) porque a coluna
 * {@code ledger_transaction_id} ja e escrita pela propria
 * {@code LedgerEntryEntity}; assim a mesma coluna nao e gerenciada duas vezes.
 */
@Entity
@Table(
        name = "ledger_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_ledger_transactions_payment_operation",
                        columnNames = {"payment_id", "operation_type"}
                )
        }
)
public class LedgerTransactionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "operation_type", nullable = false, length = 40)
    private String operationType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ledger_transaction_id", insertable = false, updatable = false)
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    /** Construtor sem argumentos exigido pelo JPA/Hibernate. */
    protected LedgerTransactionEntity() {
    }

    public LedgerTransactionEntity(UUID id,
                                   UUID paymentId,
                                   String operationType,
                                   LocalDateTime createdAt,
                                   List<LedgerEntryEntity> entries) {
        this.id = id;
        this.paymentId = paymentId;
        this.operationType = operationType;
        this.createdAt = createdAt;
        this.entries = entries;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getOperationType() {
        return operationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<LedgerEntryEntity> getEntries() {
        return entries;
    }
}
