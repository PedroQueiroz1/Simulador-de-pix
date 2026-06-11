package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseMapper;
import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.IdempotencyStartResult;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementacao (orquestradora) do caso de uso de criacao de pagamento Pix.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>gera o fingerprint (hash) do payload via {@link RequestFingerprintGenerator};</li>
 *   <li>chama {@link IdempotencyService#startOrReturn} — que reivindica a chave
 *       atomicamente (SETNX), devolve a resposta original de um retry
 *       equivalente ou lanca conflito/em-processamento;</li>
 *   <li>se for nova operacao: delega ao {@link CreatePixPaymentWriter} o passo
 *       transacional (INSERT do pagamento + evento na Outbox, ADR-025) e, apos o
 *       commit, marca a idempotencia como {@code COMPLETED} com a resposta;</li>
 *   <li>se for retry equivalente: devolve a resposta armazenada, sem recriar nada.</li>
 * </ol>
 *
 * <p><strong>Ultima barreira (corrida):</strong> se a constraint unica de
 * {@code idempotency_key} do SQL Server barrar o INSERT
 * ({@link DuplicateIdempotencyKeyException}) — possivel quando o Redis perdeu o
 * registro (restart/TTL) e duas tentativas chegam ao banco — este orquestrador
 * recupera o pagamento vencedor por {@code findByIdempotencyKey} e devolve a
 * resposta original (mesma semantica do retry equivalente), ou lanca
 * {@link IdempotencyConflictException} se o payload divergir. Este metodo NAO e
 * transacional de proposito: a captura precisa acontecer depois do rollback da
 * transacao do writer (ver javadoc de {@link CreatePixPaymentWriter}).
 *
 * <p>O {@code complete} no Redis acontece apos o commit do writer: uma transacao
 * revertida nunca deixa resposta {@code COMPLETED} para tras.
 */
public class CreatePixPaymentService implements CreatePixPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePixPaymentService.class);

    private final CreatePixPaymentWriter writer;
    private final PixPaymentRepository repository;
    private final RequestFingerprintGenerator fingerprintGenerator;
    private final IdempotencyService idempotencyService;

    public CreatePixPaymentService(CreatePixPaymentWriter writer,
                                   PixPaymentRepository repository,
                                   RequestFingerprintGenerator fingerprintGenerator,
                                   IdempotencyService idempotencyService) {
        this.writer = writer;
        this.repository = repository;
        this.fingerprintGenerator = fingerprintGenerator;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public CreatePixPaymentResult create(CreatePixPaymentCommand command) {
        String requestHash = fingerprintGenerator.generate(command);

        // Barreira de idempotencia: reivindica a chave atomicamente, devolve o
        // retry equivalente ou lanca IdempotencyConflict/InProgressException.
        IdempotencyStartResult start =
                idempotencyService.startOrReturn(command.idempotencyKey(), requestHash);

        if (start.isCompleted()) {
            // Retry equivalente: devolve a resposta original, sem recriar nada.
            CreatePixPaymentResult cached =
                    IdempotencyResponseMapper.toResult(start.response().orElseThrow());
            log.info("Idempotent retry: returning existing payment {} without recreating", cached.paymentId());
            return cached;
        }

        CreatePixPaymentResult result;
        try {
            result = writer.createNew(command);
        } catch (DuplicateIdempotencyKeyException e) {
            // Ultima barreira (constraint unica do banco): outra requisicao com a
            // mesma chave venceu. A transacao do writer ja sofreu rollback; aqui,
            // fora de transacao, recuperamos o vencedor e respondemos idempotente.
            log.info("Idempotency-Key already persisted (DB unique constraint); recovering winner payment");
            return recoverExistingPayment(command, requestHash);
        }

        // Apos o commit: guarda a resposta original no Redis para futuros retries.
        idempotencyService.complete(
                command.idempotencyKey(),
                requestHash,
                IdempotencyResponseMapper.toResponseData(result)
        );

        return result;
    }

    /**
     * Recupera o pagamento ja persistido para a {@code Idempotency-Key} e decide
     * a resposta com a mesma regra do fingerprint: payload equivalente devolve a
     * resposta original; payload divergente e conflito (409).
     */
    private CreatePixPaymentResult recoverExistingPayment(CreatePixPaymentCommand command,
                                                          String requestHash) {
        PixPayment existing = repository.findByIdempotencyKey(command.idempotencyKey())
                // Caso raro: o vencedor sofreu rollback depois de nos barrar. Sem
                // registro para devolver, sinaliza "em processamento" (409) — um
                // retry do cliente cria o pagamento normalmente.
                .orElseThrow(() -> new IdempotencyInProgressException(command.idempotencyKey()));

        // Reusa a forma canonica do fingerprint para comparar payloads: garante
        // a MESMA nocao de equivalencia da barreira do Redis (trim, escala 2...).
        String existingHash = fingerprintGenerator.generate(new CreatePixPaymentCommand(
                existing.getPayerKey(),
                existing.getReceiverKey(),
                existing.getAmount(),
                existing.getDescription(),
                existing.getIdempotencyKey()
        ));

        if (!existingHash.equals(requestHash)) {
            // Mesma chave, payload diferente: reuso indevido da chave.
            throw new IdempotencyConflictException(command.idempotencyKey());
        }

        CreatePixPaymentResult result = new CreatePixPaymentResult(
                existing.getId(),
                existing.getStatus(),
                existing.getPayerKey(),
                existing.getReceiverKey(),
                existing.getAmount(),
                existing.getDescription()
        );

        // Bonus de resiliencia: se o vencedor morreu entre o commit e o proprio
        // complete, este complete cura a chave (evita 409 "in progress" ate o TTL).
        idempotencyService.complete(
                command.idempotencyKey(),
                requestHash,
                IdempotencyResponseMapper.toResponseData(result)
        );

        log.info("Recovered existing payment {} for reused Idempotency-Key; returning original response",
                existing.getId());
        return result;
    }
}
