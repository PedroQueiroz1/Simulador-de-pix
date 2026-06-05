package io.pixsimulator.payment.adapter.in.web;

import java.util.List;

/**
 * Corpo simples de erro retornado pela API.
 *
 * <p>Contem uma {@code message} (titulo legivel) e uma lista {@code errors}
 * com os detalhes. Nunca expoe stack trace nem detalhes internos.
 */
public record ErrorResponse(
        String message,
        List<String> errors
) {
}
