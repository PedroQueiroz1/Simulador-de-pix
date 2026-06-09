package io.pixsimulator.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.in.CreateLedgerForApprovedPaymentUseCase;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.GetLedgerByPaymentUseCase;
import io.pixsimulator.payment.application.port.in.GetPixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.IdempotencyRepository;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.application.port.out.OutboxRepository;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.application.usecase.CreateLedgerForApprovedPaymentService;
import io.pixsimulator.payment.application.usecase.CreatePixPaymentService;
import io.pixsimulator.payment.application.usecase.GetLedgerByPaymentService;
import io.pixsimulator.payment.application.usecase.GetPixPaymentService;
import io.pixsimulator.payment.application.usecase.ProcessPixPaymentService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao de wiring do caso de uso.
 *
 * <p>Como o caso de uso, o servico de idempotencia e o gerador de fingerprint
 * nao conhecem Spring, e aqui (na borda de configuracao) que eles sao montados,
 * recebendo os adapters de saida ({@link PixPaymentRepository},
 * {@link IdGenerator} e {@link IdempotencyRepository}) e sendo expostos como
 * beans.
 *
 * <p>{@link IdempotencyProperties} e habilitada aqui para fornecer o TTL
 * configuravel ao {@link IdempotencyService}.
 *
 * <p>Lote 6: tambem monta o {@link PaymentOutboxEventService} (que grava eventos
 * na Outbox dentro da transacao dos casos de uso) e habilita as propriedades de
 * Kafka/Outbox ({@link KafkaTopicsProperties}, {@link OutboxPublisherProperties}).
 */
@Configuration
@EnableConfigurationProperties({
        IdempotencyProperties.class,
        KafkaTopicsProperties.class,
        OutboxPublisherProperties.class
})
public class UseCaseConfig {

    @Bean
    public RequestFingerprintGenerator requestFingerprintGenerator() {
        return new RequestFingerprintGenerator();
    }

    @Bean
    public IdempotencyService idempotencyService(IdempotencyRepository idempotencyRepository,
                                                 IdempotencyProperties properties) {
        return new IdempotencyService(idempotencyRepository, properties.ttl());
    }

    @Bean
    public PaymentOutboxEventService paymentOutboxEventService(OutboxRepository outboxRepository,
                                                               IdGenerator idGenerator,
                                                               ObjectMapper objectMapper,
                                                               KafkaTopicsProperties kafkaTopicsProperties) {
        return new PaymentOutboxEventService(
                outboxRepository, idGenerator, objectMapper, kafkaTopicsProperties.getPaymentEvents());
    }

    @Bean
    public CreatePixPaymentUseCase createPixPaymentUseCase(PixPaymentRepository repository,
                                                           IdGenerator idGenerator,
                                                           RequestFingerprintGenerator fingerprintGenerator,
                                                           IdempotencyService idempotencyService,
                                                           PaymentOutboxEventService paymentOutboxEventService) {
        return new CreatePixPaymentService(
                repository, idGenerator, fingerprintGenerator, idempotencyService, paymentOutboxEventService);
    }

    @Bean
    public GetPixPaymentUseCase getPixPaymentUseCase(PixPaymentRepository repository) {
        return new GetPixPaymentService(repository);
    }

    @Bean
    public ProcessPixPaymentUseCase processPixPaymentUseCase(
            PixPaymentRepository repository,
            CreateLedgerForApprovedPaymentUseCase createLedgerForApprovedPaymentUseCase,
            PaymentOutboxEventService paymentOutboxEventService) {
        return new ProcessPixPaymentService(
                repository, createLedgerForApprovedPaymentUseCase, paymentOutboxEventService);
    }

    // ---------------------------------------------------------------------
    // Lote 5: Ledger
    // ---------------------------------------------------------------------

    @Bean
    public CreateLedgerForApprovedPaymentUseCase createLedgerForApprovedPaymentUseCase(
            LedgerRepository ledgerRepository,
            IdGenerator idGenerator) {
        return new CreateLedgerForApprovedPaymentService(ledgerRepository, idGenerator);
    }

    @Bean
    public GetLedgerByPaymentUseCase getLedgerByPaymentUseCase(PixPaymentRepository paymentRepository,
                                                               LedgerRepository ledgerRepository) {
        return new GetLedgerByPaymentService(paymentRepository, ledgerRepository);
    }
}
