package io.pixsimulator.payment.adapter.out.messaging.kafka;

import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import io.pixsimulator.payment.config.OutboxPublisherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Publisher assincrono da Outbox (Lote 6).
 *
 * <p>Em intervalos fixos, le os eventos {@link OutboxEventStatus#PENDING} ja
 * disponiveis e os publica no Kafka. No sucesso marca como
 * {@link OutboxEventStatus#PUBLISHED}; na falha registra a tentativa (incrementa
 * {@code attempts}, guarda um erro curto e reprograma {@code availableAt}) e, ao
 * esgotar {@code max-attempts}, marca como {@link OutboxEventStatus#FAILED}.
 *
 * <p>Chave Kafka = {@code partitionKey} (paymentId); valor = {@code payload}
 * JSON. O publish e confirmado de forma sincrona ({@code .get()}) para so marcar
 * PUBLISHED apos o broker reconhecer o registro.
 *
 * <p>Desligavel por configuracao ({@code pix.outbox.publisher.enabled=false}):
 * util em testes de integracao que nao querem o polling concorrente. O modelo e
 * <strong>at-least-once</strong> (ADR-027): o consumidor idempotente vem no
 * Lote 7.
 */
@Component
@ConditionalOnProperty(prefix = "pix.outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           OutboxPublisherProperties properties) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Varredura agendada. O intervalo vem de
     * {@code pix.outbox.publisher.fixed-delay-ms}. Qualquer erro inesperado e
     * apenas logado, para nao derrubar o agendamento.
     */
    @Scheduled(fixedDelayString = "${pix.outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        try {
            publishBatch();
        } catch (Exception e) {
            log.error("Unexpected error while scanning the outbox", e);
        }
    }

    /** Le um lote de eventos PENDING e tenta publicar cada um. Visivel para teste. */
    void publishBatch() {
        List<OutboxEvent> pending =
                outboxRepository.findPendingEvents(properties.getBatchSize(), LocalDateTime.now());
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            // Chave = partitionKey (paymentId); valor = payload JSON. .get() bloqueia
            // ate o broker confirmar, para so marcar PUBLISHED apos o ack.
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload()).get();
            outboxRepository.markAsPublished(event.getId(), LocalDateTime.now());
            log.info("Published outbox event {} ({}) to topic {}",
                    event.getId(), event.getEventType(), event.getTopic());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleFailure(event, e);
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }

    private void handleFailure(OutboxEvent event, Exception e) {
        LocalDateTime nextAvailableAt = LocalDateTime.now().plus(Duration.ofMillis(properties.getFixedDelayMs()));
        // O dominio decide: incrementa attempts, guarda erro curto e vira FAILED
        // ao atingir max-attempts; senao permanece PENDING para retry.
        event.registerFailedAttempt(shortMessage(e), nextAvailableAt, properties.getMaxAttempts());
        outboxRepository.markAsFailed(event);

        if (event.getStatus() == OutboxEventStatus.FAILED) {
            log.error("Outbox event {} reached max attempts ({}) and was marked FAILED: {}",
                    event.getId(), properties.getMaxAttempts(), event.getLastError());
        } else {
            log.warn("Failed to publish outbox event {} (attempt {}); next retry after {}: {}",
                    event.getId(), event.getAttempts(), nextAvailableAt, event.getLastError());
        }
    }

    /** Mensagem curta para {@code lastError} (a truncagem em 1000 fica no dominio). */
    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        return message != null ? message : e.getClass().getName();
    }
}
