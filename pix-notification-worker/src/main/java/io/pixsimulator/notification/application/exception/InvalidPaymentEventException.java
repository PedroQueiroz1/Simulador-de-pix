package io.pixsimulator.notification.application.exception;

import java.util.UUID;

/**
 * Sinaliza um evento de pagamento invalido: JSON malformado, campo
 * obrigatorio ausente, {@code eventVersion} incompativel ou {@code eventType}
 * desconhecido.
 *
 * <p>E um erro <strong>controlado</strong>: o handler a captura e NUNCA deixa o
 * consumer cair. Carrega o {@code eventId} quando foi possivel identifica-lo no
 * payload — nesse caso o handler grava uma auditoria {@code FAILED}; quando nulo,
 * apenas loga.
 */
public class InvalidPaymentEventException extends RuntimeException {

    private final transient UUID eventId;

    public InvalidPaymentEventException(UUID eventId, String message) {
        super(message);
        this.eventId = eventId;
    }

    /** {@code eventId} do evento, ou {@code null} se nao foi possivel identifica-lo. */
    public UUID getEventId() {
        return eventId;
    }
}
