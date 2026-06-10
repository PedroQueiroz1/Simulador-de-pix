package io.github.shopkdris.pixsimulator.notification.adapter.in.kafka;

import io.github.shopkdris.pixsimulator.notification.application.consumer.PaymentEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada Kafka (Lote 7).
 *
 * <p>Unica responsabilidade: receber a mensagem do topico de eventos de
 * pagamento e delegar para o {@link PaymentEventHandler}. NAO contem regra de
 * negocio — nao parseia, nao valida, nao decide idempotencia (tudo isso vive no
 * handler). O topico e o {@code group-id} vem de configuracao (variaveis de
 * ambiente), com defaults nao sensiveis para o ambiente local.
 *
 * <p>O valor chega como {@code String} (JSON cru), no mesmo formato em que o
 * {@code pix-payment-api} publica.
 */
@Component
public class PaymentEventsConsumer {

    private final PaymentEventHandler paymentEventHandler;

    public PaymentEventsConsumer(PaymentEventHandler paymentEventHandler) {
        this.paymentEventHandler = paymentEventHandler;
    }

    @KafkaListener(
            topics = "${pix.kafka.topics.payment-events:pix.payment.events}",
            groupId = "${pix.kafka.consumer.group-id:pix-notification-worker}")
    public void onMessage(@Payload String payload) {
        paymentEventHandler.handle(payload);
    }
}
