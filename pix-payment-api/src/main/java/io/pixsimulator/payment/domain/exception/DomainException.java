package io.pixsimulator.payment.domain.exception;

/**
 * Excecao lancada quando uma regra de negocio do dominio e violada.
 *
 * <p>Exemplos: valor menor ou igual a zero, chave pagadora igual a recebedora
 * ou campos obrigatorios ausentes na criacao do pagamento.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
