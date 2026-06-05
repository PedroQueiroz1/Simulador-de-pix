package io.pixsimulator.payment.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Corpo da requisicao de criacao de pagamento Pix.
 *
 * Note que a {@code idempotencyKey} NAO faz parte do body: ela e um
 * metadado da requisicao, recebido pelo header {@code Idempotency-Key}.
 *
 * As validacoes aqui sao de formato/entrada (jakarta.validation). As regras
 * de negocio (ex.: payerKey != receiverKey) ficam no dominio.
 */
public record CreatePixPaymentRequest(

        @NotBlank(message = "payerKey e obrigatorio")
        String payerKey,

        @NotBlank(message = "receiverKey e obrigatorio")
        String receiverKey,

        @NotNull(message = "amount e obrigatorio")
        @Positive(message = "amount deve ser maior que zero")
        BigDecimal amount,

        String description
) {
}
