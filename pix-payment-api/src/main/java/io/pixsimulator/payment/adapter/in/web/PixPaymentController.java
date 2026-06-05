package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada HTTP para criacao de pagamento Pix.
 *
 * Responsabilidades: receber a request, extrair o header
 * {@code Idempotency-Key}, montar o {@link CreatePixPaymentCommand}, chamar o
 * caso de uso e converter o resultado em resposta HTTP 201.
 *
 * Nao contem regra de negocio: a regra vive no dominio.
 */
@RestController
@RequestMapping("/api/v1/pix/payments")
public class PixPaymentController {

    private final CreatePixPaymentUseCase createPixPaymentUseCase;

    public PixPaymentController(CreatePixPaymentUseCase createPixPaymentUseCase) {
        this.createPixPaymentUseCase = createPixPaymentUseCase;
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
}
