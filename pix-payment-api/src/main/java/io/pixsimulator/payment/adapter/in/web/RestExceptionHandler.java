package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de erros da API.
 *
 * Converte falhas de validacao de entrada e violacoes de regra de negocio
 * em respostas HTTP 400 com um corpo simples ({@link ErrorResponse}), sem
 * expor stack trace nem detalhes internos.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Erros de validacao do body (jakarta.validation). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse("Erro de validacao da requisicao", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Header obrigatorio ausente (ex.: Idempotency-Key). */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        List<String> errors = List.of("Header obrigatorio ausente: " + ex.getHeaderName());

        ErrorResponse body = new ErrorResponse("Erro de validacao da requisicao", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Body ausente ou JSON malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse body = new ErrorResponse(
                "Erro de validacao da requisicao",
                List.of("Corpo da requisicao ausente ou invalido"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Violacao de regra de negocio do dominio. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        ErrorResponse body = new ErrorResponse("Erro de regra de negocio", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Reuso de {@code Idempotency-Key} (Lote 2) -&gt; HTTP 409 Conflict.
     *
     * <p>O corpo nao expoe a mensagem interna da excecao: usa um texto fixo,
     * coerente com a limitacao atual (ainda nao ha comparacao de payload).
     */
    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIdempotencyKey(DuplicateIdempotencyKeyException ex) {
        ErrorResponse body = new ErrorResponse(
                "Idempotency key already used",
                List.of("A payment already exists for this Idempotency-Key"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Lote 3: {@code Idempotency-Key} reutilizada com payload diferente -&gt;
     * HTTP 409 Conflict. Mensagem fixa, sem expor detalhes internos.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        ErrorResponse body = new ErrorResponse(
                "Idempotency conflict",
                List.of("The Idempotency-Key was already used with a different request payload"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Lote 3: requisicao com {@code Idempotency-Key} cuja operacao ainda esta
     * em processamento -&gt; HTTP 409 Conflict.
     */
    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyInProgress(IdempotencyInProgressException ex) {
        ErrorResponse body = new ErrorResponse(
                "Idempotency key is already processing",
                List.of("A request with this Idempotency-Key is still being processed"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
