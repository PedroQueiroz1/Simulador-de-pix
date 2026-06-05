package io.pixsimulator.payment.domain.model;

import io.pixsimulator.payment.domain.service.PaymentDomainValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de dominio que representa um pagamento Pix.
 *
 * <p>E criada exclusivamente pela fabrica {@link #create}, que aplica as regras
 * de negocio e garante que o pagamento nasca sempre em um estado valido, com
 * status {@link PixPaymentStatus#CREATED} e {@code createdAt} preenchido.
 *
 * <p>O dominio nao depende de Spring nem da biblioteca de geracao de UUID: o
 * {@code id} ja chega pronto (gerado por uma porta {@code IdGenerator}).
 */
public final class PixPayment {

    private final UUID id;
    private final String payerKey;
    private final String receiverKey;
    private final BigDecimal amount;
    private final String description;
    private final String idempotencyKey;
    private final PixPaymentStatus status;
    private final LocalDateTime createdAt;

    private PixPayment(UUID id,
                       String payerKey,
                       String receiverKey,
                       BigDecimal amount,
                       String description,
                       String idempotencyKey,
                       PixPaymentStatus status,
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

    /**
     * Cria um novo pagamento Pix valido.
     *
     * <p>Valida as regras de negocio, define o status inicial como
     * {@link PixPaymentStatus#CREATED} e preenche o {@code createdAt}.
     *
     * @throws io.pixsimulator.payment.domain.exception.DomainException
     *         se alguma regra de negocio for violada.
     */
    public static PixPayment create(UUID id,
                                    String payerKey,
                                    String receiverKey,
                                    BigDecimal amount,
                                    String description,
                                    String idempotencyKey) {

        PaymentDomainValidator.validate(id, payerKey, receiverKey, amount, idempotencyKey);

        return new PixPayment(
                id,
                payerKey,
                receiverKey,
                amount,
                description,
                idempotencyKey,
                PixPaymentStatus.CREATED,
                LocalDateTime.now()
        );
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

    public PixPaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PixPayment)) {
            return false;
        }
        PixPayment that = (PixPayment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
