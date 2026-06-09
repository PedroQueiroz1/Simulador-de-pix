package io.pixsimulator.payment.application.outbox;

/**
 * Tipos de evento de pagamento publicados pelo {@code pix-payment-api} (Lote 6).
 *
 * <p>Sao os contratos iniciais do dominio de pagamento. O nome do enum
 * ({@link #name()}) e gravado em {@code outbox_events.event_type} e tambem no
 * campo {@code eventType} do payload JSON.
 *
 * <p>Nao existe evento separado de Ledger neste lote: quando o pagamento e
 * aprovado, o Ledger e criado na mesma transacao e o {@link #PAYMENT_APPROVED}
 * carrega o {@code ledgerTransactionId} (ADR-028 / spec 028).
 */
public enum PaymentEventType {
    PAYMENT_CREATED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED
}
