package io.pixsimulator.payment.application.port.in;

import io.pixsimulator.payment.application.dto.GetPixPaymentResult;

import java.util.UUID;

/**
 * Porta de entrada (input port) para consulta de pagamento por id (Lote 4).
 *
 * Define a intencao "consultar um pagamento" sem expor HTTP nem persistencia.
 * O adapter web depende desta interface, nao da implementacao.
 */
public interface GetPixPaymentUseCase {

    /**
     * Retorna o pagamento correspondente ao {@code paymentId}.
     *
     * @throws io.pixsimulator.payment.application.exception.PaymentNotFoundException
     *         se nao houver pagamento para o id informado.
     */
    GetPixPaymentResult getById(UUID paymentId);
}
