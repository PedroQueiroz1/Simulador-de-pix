package io.pixsimulator.payment.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA que representa a linha da tabela {@code pix_payments}.
 *
 * <p>E um detalhe de infraestrutura (camada de persistencia), separada da
 * entidade de dominio {@code domain.model.PixPayment}. So aqui vivem as
 * anotacoes JPA, nomes de coluna e constraints. A conversao entre as duas e
 * feita pelo mapper manual {@code PixPaymentJpaMapper}.
 *
 * <p>O {@code status} e persistido como {@code String} (nome do enum), nunca
 * como ordinal, para legibilidade e estabilidade do schema.
 *
 * <p>O mapeamento deve bater exatamente com o schema criado pelo Flyway, pois
 * o Hibernate roda com {@code ddl-auto=validate}.
 */
@Entity
@Table(
        name = "pix_payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_pix_payments_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
public class PixPaymentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payer_key", nullable = false, length = 120)
    private String payerKey;

    @Column(name = "receiver_key", nullable = false, length = 120)
    private String receiverKey;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Construtor sem argumentos exigido pelo JPA/Hibernate. */
    protected PixPaymentEntity() {
    }

    public PixPaymentEntity(UUID id,
                            String payerKey,
                            String receiverKey,
                            BigDecimal amount,
                            String description,
                            String idempotencyKey,
                            String status,
                            LocalDateTime createdAt) {
        this.id = id;
        this.payerKey = payerKey;
        this.receiverKey = receiverKey;
        this.amount = amount;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPayerKey() {
        return payerKey;
    }

    public String getReceiverKey() {
        return receiverKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
