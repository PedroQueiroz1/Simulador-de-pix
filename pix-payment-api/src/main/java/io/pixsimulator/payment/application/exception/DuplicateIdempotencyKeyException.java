package io.pixsimulator.payment.application.exception;

/**
 * Sinaliza que a constraint unica de {@code idempotency_key} do SQL Server
 * barrou o INSERT de um pagamento: ja existe um pagamento persistido com a
 * mesma {@code Idempotency-Key}.
 *
 * <p>E a <strong>ultima barreira</strong> contra duplicidade concorrente
 * (ADR-009): so e atingida quando a barreira do Redis nao segurou a corrida
 * (ex.: Redis reiniciado/expirado entre duas tentativas). E lancada pelo
 * adapter de persistencia ({@code JpaPixPaymentRepositoryAdapter}), que traduz
 * a violacao da constraint {@code UK_pix_payments_idempotency_key}.
 *
 * <p>Nao chega ao cliente como erro: o {@code CreatePixPaymentService} a captura
 * fora da transacao e recupera o pagamento vencedor, devolvendo a resposta
 * original (retry equivalente) ou lancando
 * {@link IdempotencyConflictException} se o payload divergir.
 *
 * <p><strong>Historico:</strong> uma excecao com este nome existiu no Lote 2
 * (idempotencia por banco), ficou morta apos o Lote 3 (Redis) e foi removida na
 * revisao final. Foi reintroduzida nesta melhoria, agora de fato lancada pelo
 * fluxo de producao (ver docs/learning/removed-and-obsolete-classes.md).
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String idempotencyKey, Throwable cause) {
        super("A payment already exists for Idempotency-Key: " + idempotencyKey, cause);
    }
}
