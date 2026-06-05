package io.pixsimulator.payment.adapter.in.web;

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
 * <p>Converte falhas de validacao de entrada e violacoes de regra de negocio
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

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
