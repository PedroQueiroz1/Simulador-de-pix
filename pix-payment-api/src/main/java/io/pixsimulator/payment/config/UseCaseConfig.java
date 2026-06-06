package io.pixsimulator.payment.config;

import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.IdempotencyRepository;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.application.usecase.CreatePixPaymentService;
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
 */
@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
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
    public CreatePixPaymentUseCase createPixPaymentUseCase(PixPaymentRepository repository,
                                                           IdGenerator idGenerator,
                                                           RequestFingerprintGenerator fingerprintGenerator,
                                                           IdempotencyService idempotencyService) {
        return new CreatePixPaymentService(repository, idGenerator, fingerprintGenerator, idempotencyService);
    }
}
