package io.pixsimulator.payment.domain.model;

/**
 * Estados possiveis de um pagamento Pix.
 *
 * <p>No Lote 1, apenas {@link #CREATED} e usado na criacao. Os demais estados
 * existem para preparar o ciclo de vida que sera processado nos proximos lotes.
 */
public enum PixPaymentStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    REJECTED
}
