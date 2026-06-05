package io.pixsimulator.payment.adapter.out.id;

import com.github.f4b6a3.uuid.UuidCreator;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter de saida que gera identificadores UUIDv7.
 *
 * E o unico ponto do projeto que conhece a biblioteca {@code uuid-creator}.
 * {@code UuidCreator.getTimeOrderedEpoch()} produz um UUID versao 7
 * (time-ordered epoch), que carrega informacao temporal e melhora ordenacao
 * e localidade em indices.
 */
@Component
public class UuidV7IdGenerator implements IdGenerator {

    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
