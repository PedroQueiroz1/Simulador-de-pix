package io.pixsimulator.payment.application.idempotency;

import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.port.out.IdempotencyRepository;

import java.time.Duration;

/**
 * Orquestra o controle de idempotencia sobre o {@link IdempotencyRepository}.
 *
 * <p>Concentra a decisao de o que fazer ao receber uma {@code Idempotency-Key},
 * sem conhecer Redis nem HTTP. O caso de uso depende deste servico, nao da
 * tecnologia de armazenamento.
 *
 * <p>Regras de {@link #startOrReturn(String, String)}:
 * <ul>
 *   <li>chave reivindicada atomicamente (nao existia): devolve "nova operacao";</li>
 *   <li>registro com hash diferente: {@link IdempotencyConflictException};</li>
 *   <li>registro {@code PROCESSING} com mesmo hash:
 *       {@link IdempotencyInProgressException};</li>
 *   <li>registro {@code COMPLETED} com mesmo hash: devolve a resposta armazenada.</li>
 * </ul>
 *
 * <p>A reivindicacao usa {@code tryStartProcessing} (SETNX), que e atomica:
 * entre N requisicoes concorrentes com a mesma chave, exatamente uma segue como
 * operacao nova — as demais leem o registro existente e caem nas regras acima.
 * Antes (leitura seguida de escrita) duas requisicoes podiam ver "chave
 * inexistente" ao mesmo tempo e ambas seguir para o INSERT no banco.
 *
 * <p>E uma classe pura (sem Spring): o TTL chega pronto por construtor, a partir
 * da configuracao, e e repassado ao repositorio em cada escrita.
 */
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final Duration ttl;

    public IdempotencyService(IdempotencyRepository repository, Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    /**
     * Inicia uma operacao idempotente ou devolve o resultado de uma ja conhecida.
     *
     * @throws IdempotencyConflictException se a chave ja foi usada com outro payload
     * @throws IdempotencyInProgressException se a operacao original ainda esta em andamento
     */
    public IdempotencyStartResult startOrReturn(String idempotencyKey, String requestHash) {
        // Duas tentativas cobrem o caso raro de a chave expirar (TTL) entre o
        // claim que falhou e a leitura seguinte: na segunda volta o claim vence.
        for (int attempt = 0; attempt < 2; attempt++) {
            if (repository.tryStartProcessing(idempotencyKey, requestHash, ttl)) {
                return IdempotencyStartResult.newOperation();
            }
            var existing = repository.findByKey(idempotencyKey);
            if (existing.isPresent()) {
                return handleExisting(idempotencyKey, requestHash, existing.get());
            }
        }
        // Claim falhou duas vezes sem registro legivel: trata como em andamento
        // (o cliente pode repetir a requisicao com seguranca).
        throw new IdempotencyInProgressException(idempotencyKey);
    }

    private IdempotencyStartResult handleExisting(String idempotencyKey,
                                                  String requestHash,
                                                  IdempotencyRecord record) {
        if (!record.requestHash().equals(requestHash)) {
            // Mesma chave, payload diferente: reuso indevido da chave.
            throw new IdempotencyConflictException(idempotencyKey);
        }

        if (record.status() == IdempotencyStatus.PROCESSING) {
            // Mesma chave e payload, mas a primeira operacao ainda nao terminou.
            throw new IdempotencyInProgressException(idempotencyKey);
        }

        // COMPLETED com mesmo hash: retry equivalente, devolve a resposta original.
        return IdempotencyStartResult.completed(record.response());
    }

    /**
     * Marca a operacao como concluida, armazenando a resposta original para
     * futuros retries equivalentes.
     */
    public void complete(String idempotencyKey, String requestHash, IdempotencyResponseData response) {
        repository.saveCompleted(idempotencyKey, requestHash, response, ttl);
    }
}
