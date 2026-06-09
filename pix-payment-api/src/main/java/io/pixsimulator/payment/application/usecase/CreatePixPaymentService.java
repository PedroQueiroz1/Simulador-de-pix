package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.idempotency.IdempotencyResponseMapper;
import io.pixsimulator.payment.application.idempotency.IdempotencyService;
import io.pixsimulator.payment.application.idempotency.IdempotencyStartResult;
import io.pixsimulator.payment.application.idempotency.RequestFingerprintGenerator;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementacao do caso de uso de criacao de pagamento Pix.
 *
 * <p>Lote 3: a idempotencia completa passa a ser controlada via Redis (atraves
 * do {@link IdempotencyService}), em vez de uma simples consulta ao banco.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>gera o fingerprint (hash) do payload via {@link RequestFingerprintGenerator};</li>
 *   <li>chama {@link IdempotencyService#startOrReturn} — que marca a chave como
 *       {@code PROCESSING}, devolve a resposta original de um retry equivalente
 *       ou lanca conflito/em-processamento;</li>
 *   <li>se for nova operacao: gera o id, cria a entidade de dominio, persiste no
 *       SQL Server e marca a idempotencia como {@code COMPLETED} com a resposta;</li>
 *   <li>se for retry equivalente: devolve a resposta armazenada, sem criar nem
 *       salvar de novo.</li>
 * </ol>
 *
 * <p>A constraint unica do SQL Server continua como barreira final contra
 * duplicidade (Redis e SQL Server nao compartilham transacao). Nao depende de
 * Spring nem de Redis: as dependencias chegam por construtor.
 *
 * <p>Lote 6 (Transactional Outbox): ao criar um novo pagamento, grava tambem o
 * evento {@code PAYMENT_CREATED} na Outbox. O metodo passa a ser
 * {@link Transactional} para que o INSERT do pagamento e o INSERT do OutboxEvent
 * fiquem na <strong>mesma transacao</strong> do SQL Server (ADR-025). No retry
 * idempotente equivalente (resposta vinda do Redis) nada e recriado: nem
 * Payment, nem OutboxEvent.
 */
public class CreatePixPaymentService implements CreatePixPaymentUseCase {

    private final PixPaymentRepository repository;
    private final IdGenerator idGenerator;
    private final RequestFingerprintGenerator fingerprintGenerator;
    private final IdempotencyService idempotencyService;
    private final PaymentOutboxEventService paymentOutboxEventService;

    public CreatePixPaymentService(PixPaymentRepository repository,
                                   IdGenerator idGenerator,
                                   RequestFingerprintGenerator fingerprintGenerator,
                                   IdempotencyService idempotencyService,
                                   PaymentOutboxEventService paymentOutboxEventService) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.fingerprintGenerator = fingerprintGenerator;
        this.idempotencyService = idempotencyService;
        this.paymentOutboxEventService = paymentOutboxEventService;
    }

    @Override
    @Transactional
    public CreatePixPaymentResult create(CreatePixPaymentCommand command) {
        String requestHash = fingerprintGenerator.generate(command);

        // Barreira de idempotencia: registra PROCESSING, devolve retry equivalente
        // ou lanca IdempotencyConflictException / IdempotencyInProgressException.
        IdempotencyStartResult start =
                idempotencyService.startOrReturn(command.idempotencyKey(), requestHash);

        if (start.isCompleted()) {
            // Retry equivalente: devolve a resposta original, sem recriar nada.
            return IdempotencyResponseMapper.toResult(start.response().orElseThrow());
        }

        UUID id = idGenerator.generate();

        PixPayment payment = PixPayment.create(
                id,
                command.payerKey(),
                command.receiverKey(),
                command.amount(),
                command.description(),
                command.idempotencyKey()
        );

        PixPayment saved = repository.save(payment);

        // Outbox na MESMA transacao do pagamento (ADR-025): se o evento falhar,
        // o INSERT do pagamento sofre rollback junto. So acontece para criacao
        // nova — o retry equivalente ja retornou acima, sem recriar evento.
        paymentOutboxEventService.recordPaymentCreated(saved);

        CreatePixPaymentResult result = new CreatePixPaymentResult(
                saved.getId(),
                saved.getStatus(),
                saved.getPayerKey(),
                saved.getReceiverKey(),
                saved.getAmount(),
                saved.getDescription()
        );

        // Guarda a resposta original no Redis para futuros retries equivalentes.
        idempotencyService.complete(
                command.idempotencyKey(),
                requestHash,
                IdempotencyResponseMapper.toResponseData(result)
        );

        return result;
    }
}
