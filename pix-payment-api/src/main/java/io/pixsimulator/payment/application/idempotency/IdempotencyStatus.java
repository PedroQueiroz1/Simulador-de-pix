package io.pixsimulator.payment.application.idempotency;

/**
 * Estado de um registro de idempotencia.
 *
 * {@link #PROCESSING}: a requisicao foi recebida e ainda esta sendo
 * processada. Uma segunda requisicao com a mesma chave deve receber HTTP 409.
 *
 * {@link #COMPLETED}: a requisicao terminou com sucesso e a resposta original
 * foi armazenada. Um retry com a mesma chave e o mesmo payload recebe a mesma
 * resposta; com payload diferente recebe HTTP 409.
 *
 * Vive na camada de aplicacao (nao no dominio) porque modela o controle de
 * retry da API, nao uma regra de negocio do pagamento.
 */
public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED
}
