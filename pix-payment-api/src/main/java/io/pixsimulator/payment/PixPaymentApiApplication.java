package io.pixsimulator.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link EnableScheduling} habilita o agendamento usado pelo
 * {@code OutboxPublisher} (varredura periodica da tabela {@code outbox_events}).
 * O publisher e desligavel por configuracao
 * ({@code pix.outbox.publisher.enabled=false}); quando desligado, nenhum metodo
 * {@code @Scheduled} fica ativo.
 */
@SpringBootApplication
@EnableScheduling
public class PixPaymentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixPaymentApiApplication.class, args);
    }
}
