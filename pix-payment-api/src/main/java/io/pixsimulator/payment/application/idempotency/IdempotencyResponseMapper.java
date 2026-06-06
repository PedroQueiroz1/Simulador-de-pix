package io.pixsimulator.payment.application.idempotency;

import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;

/**
 * Converte entre o resultado do caso de uso ({@link CreatePixPaymentResult}) e a
 * forma armazenada da resposta idempotente ({@link IdempotencyResponseData}).
 *
 * <p>Centraliza o mapeamento para que o caso de uso e o servico de idempotencia
 * nao repitam a logica de ida e volta (em especial a conversao do enum de
 * status para {@code String} e vice-versa).
 */
public final class IdempotencyResponseMapper {

    private IdempotencyResponseMapper() {
    }

    /** Resultado do caso de uso -&gt; dado a ser armazenado no Redis. */
    public static IdempotencyResponseData toResponseData(CreatePixPaymentResult result) {
        return new IdempotencyResponseData(
                result.paymentId(),
                result.status().name(),
                result.payerKey(),
                result.receiverKey(),
                result.amount(),
                result.description()
        );
    }

    /** Dado armazenado no Redis -&gt; resultado devolvido em um retry equivalente. */
    public static CreatePixPaymentResult toResult(IdempotencyResponseData data) {
        return new CreatePixPaymentResult(
                data.paymentId(),
                PixPaymentStatus.valueOf(data.status()),
                data.payerKey(),
                data.receiverKey(),
                data.amount(),
                data.description()
        );
    }
}
