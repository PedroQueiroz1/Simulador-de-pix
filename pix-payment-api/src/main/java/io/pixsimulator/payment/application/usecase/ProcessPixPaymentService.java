package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementacao do caso de uso de processamento simulado de pagamento (Lote 4).
 *
 * <p>O processamento e <strong>deterministico</strong> (ADR-016), nao representa
 * Pix real, antifraude, saldo ou Bacen. A decisao depende apenas do valor:
 *
 * <pre>
 *   amount &lt;= 5000.00 -&gt; APPROVED
 *   amount &gt;  5000.00 -&gt; REJECTED
 * </pre>
 *
 * <p>As transicoes de status sao delegadas ao dominio ({@link PixPayment}); o
 * caso de uso apenas orquestra: busca, valida elegibilidade, aplica a regra e
 * persiste. Nao depende de Spring: a porta de saida chega por construtor.
 */
public class ProcessPixPaymentService implements ProcessPixPaymentUseCase {

    /** Limite simulado de aprovacao (inclusive). */
    static final BigDecimal APPROVAL_LIMIT = new BigDecimal("5000.00");

    /** Motivo padrao de rejeicao quando o valor excede o limite simulado. */
    static final String REJECTION_REASON = "Amount exceeds the simulated approval limit";

    private final PixPaymentRepository repository;

    public ProcessPixPaymentService(PixPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProcessPixPaymentResult process(UUID paymentId) {
        PixPayment payment = repository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Pagamento ja decidido (APPROVED/REJECTED) nao pode ser reprocessado -> 409.
        if (payment.isTerminal()) {
            throw new PaymentNotProcessableException();
        }

        LocalDateTime now = LocalDateTime.now();

        payment.markAsProcessing(now);

        // Regra simulada e deterministica de decisao.
        if (payment.getAmount().compareTo(APPROVAL_LIMIT) <= 0) {
            payment.approve(now);
        } else {
            payment.reject(REJECTION_REASON, now);
        }

        PixPayment saved = repository.save(payment);

        return new ProcessPixPaymentResult(
                saved.getId(),
                saved.getStatus(),
                saved.getProcessedAt(),
                saved.getRejectionReason()
        );
    }
}
