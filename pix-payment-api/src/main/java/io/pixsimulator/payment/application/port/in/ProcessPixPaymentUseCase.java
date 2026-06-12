package io.pixsimulator.payment.application.port.in;

import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;

import java.util.UUID;

/**
 * Porta de entrada (input port) para o processamento simulado de pagamento.
 *
 * Define a intencao "processar um pagamento" sem expor HTTP nem persistencia.
 * A decisao de aprovar/rejeitar e simulada e deterministica (ver
 * {@code ProcessPixPaymentService}).
 */
public interface ProcessPixPaymentUseCase {

    /**
     * Processa o pagamento correspondente ao {@code paymentId}, transicionando-o
     * para {@code APPROVED} ou {@code REJECTED}.
     *
     * @throws io.pixsimulator.payment.application.exception.PaymentNotFoundException
     *         se nao houver pagamento para o id informado.
     * @throws io.pixsimulator.payment.application.exception.PaymentNotProcessableException
     *         se o pagamento ja estiver em status terminal.
     */
    ProcessPixPaymentResult process(UUID paymentId);
}
