package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.domain.exception.DomainException;
import io.pixsimulator.payment.observability.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de erros da API.
 *
 * <p>Converte falhas de validacao de entrada e violacoes de regra de negocio em
 * respostas HTTP com um corpo padronizado ({@link ErrorResponse}). O corpo
 * carrega {@code timestamp}, {@code status}, {@code error}, {@code message},
 * {@code errors}, {@code path} e {@code correlationId} (lido do MDC, preenchido
 * pelo {@link CorrelationIdFilter}), sem nunca expor stack trace, classe interna
 * da excecao, SQL ou secrets (Lote 8).
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Erros de validacao do body (jakarta.validation). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        return build(HttpStatus.BAD_REQUEST, "Erro de validacao da requisicao", errors, request);
    }

    /** Header obrigatorio ausente (ex.: Idempotency-Key). */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                             HttpServletRequest request) {
        List<String> errors = List.of("Header obrigatorio ausente: " + ex.getHeaderName());

        return build(HttpStatus.BAD_REQUEST, "Erro de validacao da requisicao", errors, request);
    }

    /** Body ausente ou JSON malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Erro de validacao da requisicao",
                List.of("Corpo da requisicao ausente ou invalido"), request);
    }

    /** Violacao de regra de negocio do dominio. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Erro de regra de negocio", List.of(ex.getMessage()), request);
    }

    /**
     * Lote 4: path variable com tipo invalido (ex.: {@code paymentId} que nao e
     * um UUID valido) -&gt; HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Erro de validacao da requisicao",
                List.of("Parametro invalido: " + ex.getName()), request);
    }

    /**
     * Lote 4: pagamento nao encontrado para o {@code paymentId} informado -&gt;
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Payment not found",
                List.of("No payment was found for the provided paymentId"), request);
    }

    /**
     * Lote 4: tentativa de processar um pagamento ja em status terminal -&gt;
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(PaymentNotProcessableException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotProcessable(PaymentNotProcessableException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Payment cannot be processed",
                List.of("Payment is already in a terminal status"), request);
    }

    /**
     * Lote 3: {@code Idempotency-Key} reutilizada com payload diferente -&gt;
     * HTTP 409 Conflict. Mensagem fixa, sem expor detalhes internos.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Idempotency conflict",
                List.of("The Idempotency-Key was already used with a different request payload"), request);
    }

    /**
     * Lote 3: requisicao com {@code Idempotency-Key} cuja operacao ainda esta
     * em processamento -&gt; HTTP 409 Conflict.
     */
    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyInProgress(IdempotencyInProgressException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Idempotency key is already processing",
                List.of("A request with this Idempotency-Key is still being processed"), request);
    }

    /**
     * Monta o corpo padronizado de erro. O {@code correlationId} vem do MDC
     * (definido pelo {@link CorrelationIdFilter}); o {@code path} vem da
     * requisicao. Nenhum detalhe interno (stack trace/SQL/secret) e incluido.
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                List<String> errors,
                                                HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                errors,
                request.getRequestURI(),
                MDC.get(MdcKeys.CORRELATION_ID));
        return ResponseEntity.status(status).body(body);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
