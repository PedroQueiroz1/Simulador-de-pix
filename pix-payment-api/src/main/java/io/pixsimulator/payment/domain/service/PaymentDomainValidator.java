package io.pixsimulator.payment.domain.service;

import io.pixsimulator.payment.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Concentra as regras essenciais de validacao de um pagamento Pix.
 *
 * Faz parte do dominio puro: nao depende de Spring nem de bibliotecas de
 * infraestrutura. E usado por {@code PixPayment} no momento da criacao para
 * garantir que um pagamento so nasca em um estado valido.
 */
public final class PaymentDomainValidator {

    private PaymentDomainValidator() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /**
     * Valida os dados de criacao de um pagamento Pix.
     *
     * @throws DomainException quando alguma regra de negocio e violada.
     */
    public static void validate(UUID id,
                                String payerKey,
                                String receiverKey,
                                BigDecimal amount,
                                String idempotencyKey) {

        if (id == null) {
            throw new DomainException("id e obrigatorio");
        }
        if (isBlank(payerKey)) {
            throw new DomainException("payerKey e obrigatorio");
        }
        if (isBlank(receiverKey)) {
            throw new DomainException("receiverKey e obrigatorio");
        }
        if (amount == null) {
            throw new DomainException("amount e obrigatorio");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("amount deve ser maior que zero");
        }
        if (payerKey.equals(receiverKey)) {
            throw new DomainException("payerKey nao pode ser igual a receiverKey");
        }
        if (isBlank(idempotencyKey)) {
            throw new DomainException("idempotencyKey e obrigatoria");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
