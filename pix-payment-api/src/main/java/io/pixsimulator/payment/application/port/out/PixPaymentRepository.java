package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.domain.model.PixPayment;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida (output port) para persistencia de pagamentos.
 *
 * A aplicacao depende desta interface, nao da tecnologia (ADR: implementacao
 * em SQL Server via JPA, sem alterar o caso de uso).
 */
public interface PixPaymentRepository {

    PixPayment save(PixPayment payment);

    /**
     * Busca um pagamento pela {@code Idempotency-Key} informada.
     *
     * Usado pelo caso de uso para detectar reuso de chave antes
     * de salvar (primeira barreira de idempotencia, complementada pela
     * constraint unica do banco).
     */
    Optional<PixPayment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Busca um pagamento pelo seu identificador.
     *
     * Usado pelos casos de uso de consulta e de processamento para
     * recuperar o estado atual do pagamento antes de exibir ou transicionar.
     */
    Optional<PixPayment> findById(UUID paymentId);
}
