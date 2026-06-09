package io.pixsimulator.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos topicos Kafka (prefixo {@code pix.kafka.topics}), Lote 6.
 *
 * <p>O nome do topico e configuravel por ambiente (variavel
 * {@code PIX_PAYMENT_EVENTS_TOPIC}); o default {@code pix.payment.events} cobre o
 * ambiente local.
 */
@ConfigurationProperties(prefix = "pix.kafka.topics")
public class KafkaTopicsProperties {

    /** Topico unico dos eventos de pagamento (PAYMENT_CREATED/APPROVED/REJECTED). */
    private String paymentEvents = "pix.payment.events";

    public String getPaymentEvents() {
        return paymentEvents;
    }

    public void setPaymentEvents(String paymentEvents) {
        this.paymentEvents = paymentEvents;
    }
}
