package io.pixsimulator.payment.application.port.out;

import java.util.UUID;

/**
 * Porta de saida (output port) para geracao de identificadores.
 *
 * <p>Abstrai a estrategia de geracao de UUID. O dominio e o caso de uso
 * dependem desta interface e nao da biblioteca concreta de UUIDv7, o que
 * facilita testes (gerador fake/deterministico) e troca de implementacao.
 */
public interface IdGenerator {

    UUID generate();
}
