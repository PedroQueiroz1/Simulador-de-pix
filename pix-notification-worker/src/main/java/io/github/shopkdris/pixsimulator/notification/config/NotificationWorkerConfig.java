package io.github.shopkdris.pixsimulator.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shopkdris.pixsimulator.notification.application.consumer.PaymentEventHandler;
import io.github.shopkdris.pixsimulator.notification.application.notification.NotificationSimulator;
import io.github.shopkdris.pixsimulator.notification.application.port.out.NotificationAuditRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring do nucleo do worker (Lote 7).
 *
 * <p>O {@link NotificationSimulator} e o {@link PaymentEventHandler} sao classes
 * sem Spring (testaveis com {@code new}); e aqui, na borda de configuracao, que
 * recebem suas dependencias — o {@link ObjectMapper} auto-configurado pelo Spring
 * Boot (com JavaTimeModule, mesmo formato ISO-8601 do producer) e a porta
 * {@link NotificationAuditRepository} (adapter MongoDB).
 */
@Configuration
public class NotificationWorkerConfig {

    @Bean
    public NotificationSimulator notificationSimulator() {
        return new NotificationSimulator();
    }

    @Bean
    public PaymentEventHandler paymentEventHandler(ObjectMapper objectMapper,
                                                   NotificationSimulator notificationSimulator,
                                                   NotificationAuditRepository auditRepository) {
        return new PaymentEventHandler(objectMapper, notificationSimulator, auditRepository);
    }
}
