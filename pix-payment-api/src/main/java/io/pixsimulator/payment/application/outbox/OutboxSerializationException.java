package io.pixsimulator.payment.application.outbox;

/**
 * Lancada quando a serializacao de um payload de evento para JSON falha
 * (Lote 6).
 *
 * <p>Converte a {@code JsonProcessingException} (checked) do Jackson em uma
 * excecao nao verificada da aplicacao. Na pratica nao deve ocorrer: os payloads
 * sao records simples com tipos serializaveis. Se ocorrer dentro do caso de uso
 * transacional, dispara o rollback — preservando a invariante de que nao existe
 * mudanca de estado persistida sem o respectivo OutboxEvent.
 */
public class OutboxSerializationException extends RuntimeException {

    public OutboxSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
