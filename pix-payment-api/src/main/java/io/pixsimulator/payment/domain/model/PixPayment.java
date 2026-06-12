package io.pixsimulator.payment.domain.model;

import io.pixsimulator.payment.domain.exception.DomainException;
import io.pixsimulator.payment.domain.service.PaymentDomainValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de dominio que representa um pagamento Pix.
 *
 * E criada exclusivamente pela fabrica {@link #create}, que aplica as regras
 * de negocio e garante que o pagamento nasca sempre em um estado valido, com
 * status {@link PixPaymentStatus#CREATED} e {@code createdAt} preenchido.
 *
 * <p>A entidade controla o proprio ciclo de vida. As
 * transicoes de status ({@link #markAsProcessing}, {@link #approve},
 * {@link #reject}) sao regra de negocio e vivem aqui no dominio; qualquer
 * transicao invalida lanca {@link DomainException}. Por isso os campos que
 * mudam ao longo do ciclo ({@code status}, {@code updatedAt}, {@code processedAt}
 * e {@code rejectionReason}) nao sao {@code final}.
 *
 * O dominio nao depende de Spring nem da biblioteca de geracao de UUID: o
 * {@code id} ja chega pronto (gerado por uma porta {@code IdGenerator}).
 */
public final class PixPayment {

    private final UUID id;
    private final String payerKey;
    private final String receiverKey;
    private final BigDecimal amount;
    private final String description;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;

    private PixPaymentStatus status;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
    private String rejectionReason;

    private PixPayment(UUID id,
                       String payerKey,
                       String receiverKey,
                       BigDecimal amount,
                       String description,
                       String idempotencyKey,
                       PixPaymentStatus status,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt,
                       LocalDateTime processedAt,
                       String rejectionReason) {
        this.id = id;
        this.payerKey = payerKey;
        this.receiverKey = receiverKey;
        this.amount = amount;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.processedAt = processedAt;
        this.rejectionReason = rejectionReason;
    }

    /**
     * Cria um novo pagamento Pix valido.
     *
     * <p>Valida as regras de negocio, define o status inicial como
     * {@link PixPaymentStatus#CREATED} e preenche o {@code createdAt}. O
     * {@code updatedAt} nasce igual ao {@code createdAt}; {@code processedAt} e
     * {@code rejectionReason} comecam nulos.
     *
     * @throws DomainException se alguma regra de negocio for violada.
     */
    public static PixPayment create(UUID id,
                                    String payerKey,
                                    String receiverKey,
                                    BigDecimal amount,
                                    String description,
                                    String idempotencyKey) {

        PaymentDomainValidator.validate(id, payerKey, receiverKey, amount, idempotencyKey);

        LocalDateTime now = LocalDateTime.now();

        return new PixPayment(
                id,
                payerKey,
                receiverKey,
                amount,
                description,
                idempotencyKey,
                PixPaymentStatus.CREATED,
                now,
                now,
                null,
                null
        );
    }

    /**
     * Reconstroi um pagamento ja existente a partir de dados persistidos.
     *
     * <p>Diferente de {@link #create}, NAO gera novo {@code status} nem novas
     * datas: todos os campos chegam prontos do repositorio. E o ponto de
     * entrada usado pelo mapper de persistencia (Entity -&gt; Domain) para
     * preservar fielmente o estado salvo, sem reaplicar regras de criacao nem
     * de transicao.
     *
     * <p>Continua sendo dominio puro: nao depende de JPA nem de Spring; apenas
     * recebe os campos ja conhecidos.
     */
    public static PixPayment restore(UUID id,
                                     String payerKey,
                                     String receiverKey,
                                     BigDecimal amount,
                                     String description,
                                     String idempotencyKey,
                                     PixPaymentStatus status,
                                     LocalDateTime createdAt,
                                     LocalDateTime updatedAt,
                                     LocalDateTime processedAt,
                                     String rejectionReason) {
        return new PixPayment(
                id,
                payerKey,
                receiverKey,
                amount,
                description,
                idempotencyKey,
                status,
                createdAt,
                updatedAt,
                processedAt,
                rejectionReason
        );
    }

    /**
     * Move o pagamento de {@link PixPaymentStatus#CREATED} para
     * {@link PixPaymentStatus#PROCESSING} e atualiza o {@code updatedAt}.
     *
     * @throws DomainException se o pagamento nao estiver em {@code CREATED}.
     */
    public void markAsProcessing(LocalDateTime now) {
        if (this.status != PixPaymentStatus.CREATED) {
            throw new DomainException(
                    "Invalid transition: only a CREATED payment can move to PROCESSING (current=" + this.status + ")");
        }
        this.status = PixPaymentStatus.PROCESSING;
        this.updatedAt = now;
    }

    /**
     * Aprova o pagamento: transita de {@link PixPaymentStatus#PROCESSING} para
     * {@link PixPaymentStatus#APPROVED}, atualizando {@code updatedAt} e
     * {@code processedAt}.
     *
     * @throws DomainException se o pagamento nao estiver em {@code PROCESSING}.
     */
    public void approve(LocalDateTime now) {
        if (this.status != PixPaymentStatus.PROCESSING) {
            throw new DomainException(
                    "Invalid transition: only a PROCESSING payment can be approved (current=" + this.status + ")");
        }
        this.status = PixPaymentStatus.APPROVED;
        this.updatedAt = now;
        this.processedAt = now;
    }

    /**
     * Rejeita o pagamento: transita de {@link PixPaymentStatus#PROCESSING} para
     * {@link PixPaymentStatus#REJECTED}, atualizando {@code updatedAt},
     * {@code processedAt} e registrando o {@code rejectionReason}.
     *
     * @throws DomainException se o pagamento nao estiver em {@code PROCESSING}.
     */
    public void reject(String reason, LocalDateTime now) {
        if (this.status != PixPaymentStatus.PROCESSING) {
            throw new DomainException(
                    "Invalid transition: only a PROCESSING payment can be rejected (current=" + this.status + ")");
        }
        this.status = PixPaymentStatus.REJECTED;
        this.updatedAt = now;
        this.processedAt = now;
        this.rejectionReason = reason;
    }

    /**
     * Indica se o pagamento esta em estado terminal
     * ({@link PixPaymentStatus#APPROVED} ou {@link PixPaymentStatus#REJECTED}),
     * a partir do qual nao ha mais transicoes.
     */
    public boolean isTerminal() {
        return this.status == PixPaymentStatus.APPROVED
                || this.status == PixPaymentStatus.REJECTED;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
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
