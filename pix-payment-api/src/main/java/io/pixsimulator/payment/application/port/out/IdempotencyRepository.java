package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.application.idempotency.IdempotencyRecord;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseData;

import java.time.Duration;
import java.util.Optional;

/**
 * Porta de saida para armazenamento dos registros de idempotencia.
 *
 * <p>A aplicacao depende desta interface, nao da tecnologia (Redis). Permite:
 * buscar o registro de uma chave, reivindica-la atomicamente como
 * {@code PROCESSING} ao iniciar e marca-la como {@code COMPLETED} com a
 * resposta original ao concluir.
 *
 * <p>O TTL e responsabilidade de quem chama (o {@code IdempotencyService}), que
 * conhece a configuracao; o adapter apenas o aplica no armazenamento.
 */
public interface IdempotencyRepository {

    /** Busca o registro da chave; vazio se inexistente (ou expirado por TTL). */
    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    /**
     * Tenta registrar a chave como {@code PROCESSING}, com o hash do payload e
     * TTL, de forma <strong>atomica</strong> (so grava se a chave ainda nao
     * existir — SETNX no Redis). E a barreira que impede duas requisicoes
     * concorrentes com a mesma chave de seguirem ambas como "operacao nova".
     *
     * @return {@code true} se esta chamada reivindicou a chave (operacao nova);
     *         {@code false} se a chave ja existia (outra operacao chegou antes).
     */
    boolean tryStartProcessing(String idempotencyKey, String requestHash, Duration ttl);

    /** Registra a chave como {@code COMPLETED}, com a resposta original e TTL. */
    void saveCompleted(String idempotencyKey, String requestHash, IdempotencyResponseData response, Duration ttl);
}
