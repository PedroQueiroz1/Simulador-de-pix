package io.pixsimulator.payment.observability;

/**
 * Chaves de contexto de log (MDC) e nomes de header usados na rastreabilidade.
 *
 * <p>Centraliza as constantes para que o {@code CorrelationIdFilter}, o
 * {@code RestExceptionHandler}, os casos de uso e o
 * {@code PaymentOutboxEventService} usem exatamente os mesmos nomes. Os valores
 * do MDC sao impressos no padrao de log (ver {@code application.yml}) e
 * propagados para os eventos Kafka, ligando os logs de toda a jornada
 * HTTP -&gt; Outbox -&gt; Kafka -&gt; Worker -&gt; MongoDB.
 *
 * <p>Nenhum dado sensivel (senha, token, secret, connection string) e colocado
 * no MDC: apenas identificadores tecnicos de correlacao.
 */
public final class MdcKeys {

    /** Header HTTP de entrada/saida do correlationId. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Header HTTP de entrada/saida do requestId. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Correlaciona toda a jornada de uma operacao (atravessa servicos). */
    public static final String CORRELATION_ID = "correlationId";

    /** Identifica uma unica requisicao HTTP (nao atravessa servicos). */
    public static final String REQUEST_ID = "requestId";

    /** Pagamento associado ao processamento corrente, quando aplicavel. */
    public static final String PAYMENT_ID = "paymentId";

    /** Evento de outbox/Kafka associado, quando aplicavel. */
    public static final String OUTBOX_EVENT_ID = "outboxEventId";

    /** Transacao de Ledger associada, quando aplicavel. */
    public static final String LEDGER_TRANSACTION_ID = "ledgerTransactionId";

    private MdcKeys() {
    }
}
