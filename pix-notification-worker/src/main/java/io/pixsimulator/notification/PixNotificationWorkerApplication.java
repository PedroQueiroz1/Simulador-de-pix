package io.pixsimulator.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do {@code pix-notification-worker}.
 *
 * <p>Segundo microservico do projeto: um worker orientado a eventos, SEM
 * endpoint REST. Consome os eventos de pagamento publicados pelo
 * {@code pix-payment-api} no Kafka, simula uma notificacao e registra a
 * auditoria no MongoDB. O consumo e idempotente por {@code eventId} (Kafka
 * entrega <em>at-least-once</em>).
 */
@SpringBootApplication
public class PixNotificationWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixNotificationWorkerApplication.class, args);
    }
}
