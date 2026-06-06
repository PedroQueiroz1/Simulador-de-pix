package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.GetPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.port.in.GetPixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;

import java.util.UUID;

/**
 * Implementacao do caso de uso de consulta de pagamento por id (Lote 4).
 *
 * <p>Apenas le do repositorio e converte o dominio em
 * {@link GetPixPaymentResult}. Se o pagamento nao existir, lanca
 * {@link PaymentNotFoundException} (mapeada para HTTP 404). Nao depende de
 * Spring: a porta de saida chega por construtor.
 */
public class GetPixPaymentService implements GetPixPaymentUseCase {

    private final PixPaymentRepository repository;

    public GetPixPaymentService(PixPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public GetPixPaymentResult getById(UUID paymentId) {
        PixPayment payment = repository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return new GetPixPaymentResult(
                payment.getId(),
                payment.getStatus(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getProcessedAt(),
                payment.getRejectionReason()
        );
    }
}
