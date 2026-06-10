package io.github.shopkdris.pixsimulator.notification.application.exception;

import java.util.UUID;

/**
 * Sinaliza que um {@code eventId} ja auditado tentou ser salvo novamente (Lote 7).
 *
 * <p>E a traducao, na porta de saida, da violacao do indice unico de
 * {@code eventId} no MongoDB ({@code DuplicateKeyException}). Protege contra a
 * corrida em que duas entregas do mesmo evento passam pelo
 * {@code existsByEventId} antes de qualquer uma gravar: a segunda gravacao falha
 * e o handler trata como duplicado seguro, sem derrubar o consumer.
 */
public class DuplicateEventException extends RuntimeException {

    private final transient UUID eventId;

    public DuplicateEventException(UUID eventId, Throwable cause) {
        super("Notification audit already exists for eventId " + eventId, cause);
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
