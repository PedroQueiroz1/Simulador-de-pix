package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.dto.GetPixPaymentResult;
import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.GetPixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adapter de entrada HTTP para pagamentos Pix.
 *
 * <p>Responsabilidades: receber a request, traduzir para os comandos da
 * aplicacao, chamar os casos de uso e converter os resultados em respostas HTTP.
 * Nao contem regra de negocio: a regra vive no dominio.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/pix/payments} — cria um pagamento (Lotes 1-3);</li>
 *   <li>{@code GET /api/v1/pix/payments/{paymentId}} — consulta por id (Lote 4);</li>
 *   <li>{@code POST /api/v1/pix/payments/{paymentId}/process} — processamento
 *       simulado (Lote 4).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pix/payments")
public class PixPaymentController {

    private final CreatePixPaymentUseCase createPixPaymentUseCase;
    private final GetPixPaymentUseCase getPixPaymentUseCase;
    private final ProcessPixPaymentUseCase processPixPaymentUseCase;

    public PixPaymentController(CreatePixPaymentUseCase createPixPaymentUseCase,
                                GetPixPaymentUseCase getPixPaymentUseCase,
                                ProcessPixPaymentUseCase processPixPaymentUseCase) {
        this.createPixPaymentUseCase = createPixPaymentUseCase;
        this.getPixPaymentUseCase = getPixPaymentUseCase;
        this.processPixPaymentUseCase = processPixPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<CreatePixPaymentResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePixPaymentRequest request) {

        CreatePixPaymentCommand command = new CreatePixPaymentCommand(
                request.payerKey(),
                request.receiverKey(),
                request.amount(),
                request.description(),
                idempotencyKey
        );

        CreatePixPaymentResult result = createPixPaymentUseCase.create(command);

        CreatePixPaymentResponse response = new CreatePixPaymentResponse(
                result.paymentId(),
                result.status(),
                result.payerKey(),
                result.receiverKey(),
                result.amount(),
                result.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta um pagamento pelo seu id (Lote 4).
     *
     * <p>O Spring converte automaticamente o path variable para {@link UUID}; um
     * valor invalido resulta em HTTP 400 (tratado no {@code RestExceptionHandler}).
     * Pagamento inexistente resulta em HTTP 404.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<GetPixPaymentResponse> getById(@PathVariable UUID paymentId) {
        GetPixPaymentResult result = getPixPaymentUseCase.getById(paymentId);

        GetPixPaymentResponse response = new GetPixPaymentResponse(
                result.paymentId(),
                result.status(),
                result.payerKey(),
                result.receiverKey(),
                result.amount(),
                result.description(),
                result.createdAt(),
                result.updatedAt(),
                result.processedAt(),
                result.rejectionReason()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Processa (simuladamente) um pagamento pelo seu id (Lote 4).
     *
     * <p>O body e opcional e ignorado neste lote. Pagamento inexistente resulta
     * em HTTP 404; pagamento ja em status terminal resulta em HTTP 409.
     */
    @PostMapping("/{paymentId}/process")
    public ResponseEntity<ProcessPixPaymentResponse> process(@PathVariable UUID paymentId) {
        ProcessPixPaymentResult result = processPixPaymentUseCase.process(paymentId);

        ProcessPixPaymentResponse response = new ProcessPixPaymentResponse(
                result.paymentId(),
                result.status(),
                result.processedAt(),
                result.rejectionReason()
        );

        return ResponseEntity.ok(response);
    }
}
