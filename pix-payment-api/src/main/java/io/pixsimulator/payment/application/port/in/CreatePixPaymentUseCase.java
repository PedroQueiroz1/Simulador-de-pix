package io.pixsimulator.payment.application.port.in;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;

/**
 * Porta de entrada (input port) da aplicacao.
 *
 * Define a intencao "criar um pagamento Pix" sem expor detalhes de HTTP,
 * persistencia ou geracao de ID. Adapters de entrada (controller, futuramente
 * Kafka/CLI) dependem desta interface, nao da implementacao.
 */
public interface CreatePixPaymentUseCase {

    CreatePixPaymentResult create(CreatePixPaymentCommand command);
}
