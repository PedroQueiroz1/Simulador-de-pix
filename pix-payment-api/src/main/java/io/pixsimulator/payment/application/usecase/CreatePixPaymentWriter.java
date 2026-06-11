package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.observability.MdcKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Passo transacional da criacao de pagamento: INSERT do {@link PixPayment} e
 * gravacao do evento {@code PAYMENT_CREATED} na Outbox, na MESMA transacao do
 * SQL Server (ADR-025).
 *
 * <p>Vive em uma classe separada do {@code CreatePixPaymentService} por um
 * motivo transacional preciso: quando a constraint unica de
 * {@code idempotency_key} dispara (corrida concorrente,
 * {@link DuplicateIdempotencyKeyException}), a transacao corrente fica marcada
 * como <em>rollback-only</em> — capturar a excecao e seguir <em>dentro</em> da
 * mesma transacao terminaria em {@code UnexpectedRollbackException} no commit.
 * Com a fronteira {@link Transactional} aqui, o orquestrador (sem transacao)
 * captura a excecao <em>depois</em> do rollback e recupera o pagamento
 * vencedor em uma leitura nova.
 *
 * <p>Como as demais classes de aplicacao, nao conhece Redis nem HTTP; alem da
 * anotacao {@code @Transactional}, as dependencias chegam por construtor.
 */
public class CreatePixPaymentWriter {

    private static final Logger log = LoggerFactory.getLogger(CreatePixPaymentWriter.class);

    private final PixPaymentRepository repository;
    private final IdGenerator idGenerator;
    private final PaymentOutboxEventService paymentOutboxEventService;

    public CreatePixPaymentWriter(PixPaymentRepository repository,
                                  IdGenerator idGenerator,
                                  PaymentOutboxEventService paymentOutboxEventService) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.paymentOutboxEventService = paymentOutboxEventService;
    }

    /**
     * Cria e persiste um novo pagamento com seu evento de Outbox, atomicamente.
     *
     * @throws DuplicateIdempotencyKeyException se a constraint unica de
     *         idempotency_key barrar o INSERT (outra requisicao venceu a corrida);
     *         a transacao desta chamada sofre rollback completo.
     */
    @Transactional
    public CreatePixPaymentResult createNew(CreatePixPaymentCommand command) {
        UUID id = idGenerator.generate();
        MDC.put(MdcKeys.PAYMENT_ID, id.toString());

        PixPayment payment = PixPayment.create(
                id,
                command.payerKey(),
                command.receiverKey(),
                command.amount(),
                command.description(),
                command.idempotencyKey()
        );

        PixPayment saved = repository.save(payment);
        log.info("Created payment {} with status {}", saved.getId(), saved.getStatus());

        // Outbox na MESMA transacao do pagamento (ADR-025): se o evento falhar,
        // o INSERT do pagamento sofre rollback junto.
        paymentOutboxEventService.recordPaymentCreated(saved);

        return new CreatePixPaymentResult(
                saved.getId(),
                saved.getStatus(),
                saved.getPayerKey(),
                saved.getReceiverKey(),
                saved.getAmount(),
                saved.getDescription()
        );
    }
}
