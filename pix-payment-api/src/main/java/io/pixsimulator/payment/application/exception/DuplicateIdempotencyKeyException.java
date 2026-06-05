package io.pixsimulator.payment.application.exception;

/**
 * Lancada pelo caso de uso quando ja existe um pagamento para a
 * {@code Idempotency-Key} informada.
 *
 * <p>E uma excecao de aplicacao (nao de dominio): o dominio sabe validar que a
 * chave existe e nao e vazia, mas nao sabe se ela ja foi usada no banco. Quem
 * descobre o reuso e o caso de uso, consultando o repositorio.
 *
 * <p>O {@code RestExceptionHandler} converte esta excecao em HTTP 409 Conflict.
 * No Lote 2 esta e a primeira barreira de idempotencia (complementada pela
 * constraint unica do banco); a idempotencia completa (hash do payload, retorno
 * da resposta original) fica para o Lote 3.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("A payment already exists for Idempotency-Key: " + idempotencyKey);
    }
}
