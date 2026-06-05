package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.domain.model.PixPayment;

import java.util.Optional;

/**
 * Porta de saida (output port) para persistencia de pagamentos.
 *
 * <p>A aplicacao depende desta interface, nao da tecnologia. No Lote 1 a
 * implementacao era em memoria; no Lote 2 e SQL Server (via JPA), sem alterar
 * o caso de uso.
 */
public interface PixPaymentRepository {

    PixPayment save(PixPayment payment);

    /**
     * Busca um pagamento pela {@code Idempotency-Key} informada.
     *
     * <p>No Lote 2 e usado pelo caso de uso para detectar reuso de chave antes
     * de salvar (primeira barreira de idempotencia, complementada pela
     * constraint unica do banco). Sera evoluido no Lote 3.
     */
    Optional<PixPayment> findByIdempotencyKey(String idempotencyKey);
}
