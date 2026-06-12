package io.pixsimulator.payment.domain.model;

/**
 * Estados possiveis de um pagamento Pix.
 *
 * <p>Um pagamento nasce em {@link #CREATED}. O ciclo de vida permite
 * apenas as transicoes {@code CREATED -> PROCESSING -> APPROVED} e
 * {@code CREATED -> PROCESSING -> REJECTED}. {@link #APPROVED} e {@link #REJECTED}
 * sao estados terminais. As regras de transicao sao controladas pela entidade de
 * dominio {@code PixPayment}.
 */
public enum PixPaymentStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    REJECTED
}
