package io.pixsimulator.payment.adapter.out.messaging.kafka;

import io.pixsimulator.payment.application.outbox.OutboxEvent;
import io.pixsimulator.payment.application.outbox.OutboxEventStatus;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import io.pixsimulator.payment.config.OutboxPublisherProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link OutboxPublisher} com {@link KafkaTemplate} e
 * {@link OutboxRepository} mockados (Lote 6).
 *
 * <p>Cobre: publicacao de um evento PENDING, marcacao como PUBLISHED no sucesso,
 * registro de falha (mantendo PENDING) e transicao para FAILED ao atingir o
 * limite de tentativas, e o uso correto de topic/partitionKey/payload.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TOPIC = "pix.payment.events";
    private static final String PARTITION_KEY = "01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41";
    private static final String PAYLOAD = "{\"eventId\":\"0197a0e8-6e3f-7e2f-b3a5-bce754c21a19\"}";

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherProperties properties;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new OutboxPublisherProperties();
        properties.setBatchSize(10);
        properties.setMaxAttempts(5);
        properties.setFixedDelayMs(5000);
        publisher = new OutboxPublisher(outboxRepository, kafkaTemplate, properties);
    }

    private OutboxEvent pendingEvent() {
        return OutboxEvent.create(
                UUID.randomUUID(), "PAYMENT", UUID.fromString(PARTITION_KEY),
                "PAYMENT_CREATED", 1, TOPIC, PARTITION_KEY, PAYLOAD, LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private void stubSuccessfulSend() {
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
    }

    private void stubFailedSend(String message) {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException(message)));
    }

    @Test
    @DisplayName("Deve publicar evento PENDING no Kafka")
    void shouldPublishPendingEventToKafka() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPendingEvents(anyInt(), any())).thenReturn(List.of(event));
        stubSuccessfulSend();

        publisher.publishBatch();

        verify(kafkaTemplate).send(TOPIC, PARTITION_KEY, PAYLOAD);
    }

    @Test
    @DisplayName("Deve marcar como PUBLISHED apos sucesso")
    void shouldMarkAsPublishedAfterSuccess() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPendingEvents(anyInt(), any())).thenReturn(List.of(event));
        stubSuccessfulSend();

        publisher.publishBatch();

        verify(outboxRepository).markAsPublished(eq(event.getId()), any(LocalDateTime.class));
        verify(outboxRepository, never()).markAsFailed(any());
    }

    @Test
    @DisplayName("Deve registrar falha (mantendo PENDING) quando KafkaTemplate lancar excecao")
    void shouldRegisterFailureWhenKafkaThrows() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPendingEvents(anyInt(), any())).thenReturn(List.of(event));
        stubFailedSend("kafka down");

        publisher.publishBatch();

        // attempts incrementado, erro registrado, ainda PENDING (max=5).
        verify(outboxRepository).markAsFailed(argThat(e ->
                e.getStatus() == OutboxEventStatus.PENDING
                        && e.getAttempts() == 1
                        && e.getLastError() != null));
        verify(outboxRepository, never()).markAsPublished(any(), any());
    }

    @Test
    @DisplayName("Deve marcar como FAILED ao atingir max attempts")
    void shouldMarkAsFailedWhenMaxAttemptsReached() {
        properties.setMaxAttempts(1); // uma falha ja esgota as tentativas
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPendingEvents(anyInt(), any())).thenReturn(List.of(event));
        stubFailedSend("kafka down");

        publisher.publishBatch();

        verify(outboxRepository).markAsFailed(argThat(e -> e.getStatus() == OutboxEventStatus.FAILED));
    }

    @Test
    @DisplayName("Deve usar topic, partitionKey e payload corretos")
    void shouldUseCorrectTopicPartitionKeyAndPayload() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPendingEvents(anyInt(), any())).thenReturn(List.of(event));
        stubSuccessfulSend();

        publisher.publishBatch();

        // Chave Kafka = partitionKey; valor = payload JSON.
        verify(kafkaTemplate).send(eq(TOPIC), eq(PARTITION_KEY), eq(PAYLOAD));
    }
}
