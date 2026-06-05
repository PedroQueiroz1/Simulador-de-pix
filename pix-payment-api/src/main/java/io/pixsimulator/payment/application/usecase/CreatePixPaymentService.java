package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;

import java.util.UUID;

/**
 * Implementacao do caso de uso de criacao de pagamento Pix.
 *
 * Orquestra o fluxo: gera o id via {@link IdGenerator}, cria a entidade de
 * dominio aplicando as regras de negocio, persiste via
 * {@link PixPaymentRepository} e devolve um {@link CreatePixPaymentResult}.
 *
 * Nao depende de Spring nem da tecnologia concreta de persistencia/ID: as
 * dependencias chegam por construtor (injetadas pela configuracao).
 */
public class CreatePixPaymentService implements CreatePixPaymentUseCase {

    private final PixPaymentRepository repository;
    private final IdGenerator idGenerator;

    public CreatePixPaymentService(PixPaymentRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Override
    public CreatePixPaymentResult create(CreatePixPaymentCommand command) {
        // Lote 2: primeira barreira de idempotencia. Consulta a chave antes de
        // salvar; se ja existir, rejeita com 409. A constraint unica do banco e
        // a barreira final contra requisicoes concorrentes.
        repository.findByIdempotencyKey(command.idempotencyKey())
                .ifPresent(existing -> {
                    throw new DuplicateIdempotencyKeyException(command.idempotencyKey());
                });

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
