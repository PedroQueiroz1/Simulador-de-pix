package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.domain.model.PixPayment;

/**
 * Porta de saida (output port) para persistencia de pagamentos.
 *
 * <p>A aplicacao depende desta interface, nao da tecnologia. No Lote 1 a
 * implementacao e em memoria; no Lote 2 sera SQL Server, sem alterar o caso
 * de uso.
 */
public interface PixPaymentRepository {

    PixPayment save(PixPayment payment);
}
