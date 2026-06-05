package io.pixsimulator.payment.config;

import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.application.usecase.CreatePixPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao de wiring do caso de uso.
 *
 * Como o caso de uso e o dominio nao conhecem Spring, e aqui (na borda de
 * configuracao) que o {@link CreatePixPaymentService} e montado, recebendo os
 * adapters de saida ({@link PixPaymentRepository} e {@link IdGenerator}) e
 * sendo exposto como a porta de entrada {@link CreatePixPaymentUseCase}.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreatePixPaymentUseCase createPixPaymentUseCase(PixPaymentRepository repository,
                                                           IdGenerator idGenerator) {
        return new CreatePixPaymentService(repository, idGenerator);
    }
}
