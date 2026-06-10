package io.pixsimulator.payment.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Corpo padronizado de erro da API (Lote 8).
 *
 * <p>Evolui o formato anterior ({@code message} + {@code errors}) acrescentando
 * campos uteis para sustentacao e rastreabilidade:
 * <ul>
 *   <li>{@code timestamp} — quando o erro foi montado;</li>
 *   <li>{@code status} — codigo HTTP (ex.: 400, 404, 409);</li>
 *   <li>{@code error} — reason phrase do status (ex.: "Conflict");</li>
 *   <li>{@code message} — titulo legivel;</li>
 *   <li>{@code errors} — detalhes (ex.: erros de validacao por campo);</li>
 *   <li>{@code path} — URI da requisicao;</li>
 *   <li>{@code correlationId} — liga este erro aos logs da jornada.</li>
 * </ul>
 *
 * <p>NUNCA expoe stack trace, classe interna da excecao, SQL, secrets nem o
 * payload completo. {@link JsonInclude} omite campos nulos (ex.: correlationId
 * ausente em algum cenario de teste sem o filtro).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> errors,
        String path,
        String correlationId
) {
}
