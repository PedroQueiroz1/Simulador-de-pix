package io.pixsimulator.notification.domain;

import java.util.Optional;

/**
 * Tipos de evento de pagamento que o worker sabe processar (Lote 7).
 *
 * <p>Espelham o contrato publicado pelo {@code pix-payment-api} (Lote 6). Um
 * {@code eventType} fora desta lista e tratado como erro controlado (tipo
 * desconhecido), nunca derrubando o consumer.
 */
public enum NotificationEventType {
    PAYMENT_CREATED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED;

    /**
     * Resolve o enum a partir do {@code eventType} bruto do payload. Retorna
     * vazio (em vez de lancar) para que o handler trate tipo desconhecido como
     * erro controlado.
     */
    public static Optional<NotificationEventType> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (NotificationEventType type : values()) {
            if (type.name().equals(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
