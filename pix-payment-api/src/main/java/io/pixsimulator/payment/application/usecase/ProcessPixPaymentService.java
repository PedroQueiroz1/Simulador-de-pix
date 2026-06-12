package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.outbox.PaymentOutboxEventService;
import io.pixsimulator.payment.application.port.in.CreateLedgerForApprovedPaymentUseCase;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.observability.MdcKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementacao do caso de uso de processamento simulado de pagamento,
 * integrado ao Ledger.
 *
 * <p>O processamento e <strong>deterministico</strong> (ADR-016), nao representa
 * Pix real, antifraude, saldo ou Bacen. A decisao depende apenas do valor:
 *
 * <pre>
 *   amount &lt;= 5000.00 -&gt; APPROVED  (gera Ledger PIX_SETTLEMENT)
 *   amount &gt;  5000.00 -&gt; REJECTED  (nao gera Ledger)
 * </pre>
 *
 * <p><strong>Atomicidade:</strong> aprovar o pagamento e criar o Ledger
 * acontecem na mesma transacao ({@link Transactional}). Nao pode existir
 * pagamento aprovado sem Ledger, nem Ledger sem pagamento aprovado. Os saves do
 * pagamento e do ledger (REQUIRED) participam desta mesma transacao local do
 * SQL Server (ADR-021). Se a criacao do Ledger falhar, a aprovacao do pagamento
 * sofre rollback junto.
 *
 * <p>As transicoes de status sao delegadas ao dominio ({@link PixPayment}); o
 * caso de uso apenas orquestra: busca, valida elegibilidade, aplica a regra,
 * persiste e (quando aprovado) dispara a criacao do Ledger.
 *
 * <p>Transactional Outbox: dentro da mesma transacao, grava o evento
 * correspondente na Outbox — {@code PAYMENT_APPROVED} (com o
 * {@code ledgerTransactionId} do settlement) ou {@code PAYMENT_REJECTED} (com o
 * {@code rejectionReason}). Invariantes: pagamento aprovado nunca fica sem
 * Ledger nem sem OutboxEvent; pagamento rejeitado nunca gera Ledger, mas sempre
 * gera OutboxEvent.
 */
public class ProcessPixPaymentService implements ProcessPixPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPixPaymentService.class);

    /** Limite simulado de aprovacao (inclusive). */
    static final BigDecimal APPROVAL_LIMIT = new BigDecimal("5000.00");

    /** Motivo padrao de rejeicao quando o valor excede o limite simulado. */
    static final String REJECTION_REASON = "Amount exceeds the simulated approval limit";

    private final PixPaymentRepository repository;
    private final CreateLedgerForApprovedPaymentUseCase createLedgerForApprovedPayment;
    private final PaymentOutboxEventService paymentOutboxEventService;

    public ProcessPixPaymentService(PixPaymentRepository repository,
                                    CreateLedgerForApprovedPaymentUseCase createLedgerForApprovedPayment,
                                    PaymentOutboxEventService paymentOutboxEventService) {
        this.repository = repository;
        this.createLedgerForApprovedPayment = createLedgerForApprovedPayment;
        this.paymentOutboxEventService = paymentOutboxEventService;
    }

    @Override
    @Transactional
    public ProcessPixPaymentResult process(UUID paymentId) {
        MDC.put(MdcKeys.PAYMENT_ID, paymentId.toString());

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
            PixPayment saved = repository.save(payment);
            // Mesma transacao: aprovado => Ledger obrigatorio.
            LedgerTransaction ledger = createLedgerForApprovedPayment.createForApprovedPayment(saved);
            // Mesma transacao: aprovado => OutboxEvent obrigatorio, carregando o
            // ledgerTransactionId do settlement recem-criado.
            paymentOutboxEventService.recordPaymentApproved(saved, ledger.getId());
            log.info("Payment {} APPROVED; ledger transaction {} created", saved.getId(), ledger.getId());
            return toResult(saved);
        }

        payment.reject(REJECTION_REASON, now);
        PixPayment saved = repository.save(payment);
        // Rejeitado nao movimenta valor: nenhum Ledger e criado, mas o evento
        // PAYMENT_REJECTED e gravado na mesma transacao.
        paymentOutboxEventService.recordPaymentRejected(saved);
        log.info("Payment {} REJECTED: {}", saved.getId(), saved.getRejectionReason());
        return toResult(saved);
    }

    private ProcessPixPaymentResult toResult(PixPayment payment) {
        return new ProcessPixPaymentResult(
                payment.getId(),
                payment.getStatus(),
                payment.getProcessedAt(),
                payment.getRejectionReason()
        );
    }
}
