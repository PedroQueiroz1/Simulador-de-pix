package io.pixsimulator.payment.application.port.out;

import io.pixsimulator.payment.application.outbox.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Porta de saida (output port) do Transactional Outbox.
 *
 * <p>Abstrai a persistencia dos {@link OutboxEvent}. O {@code save} participa da
 * transacao do caso de uso (mesma transacao da mudanca de Payment/Ledger,
 * ADR-025); os demais metodos sao usados pelo publisher assincrono.
 *
 * <p>A aplicacao depende desta interface, nao do adapter JPA concreto.
 */
public interface OutboxRepository {

    /** Persiste um novo evento (na transacao corrente do caso de uso). */
    OutboxEvent save(OutboxEvent event);

    /**
     * Busca ate {@code limit} eventos {@link io.pixsimulator.payment.application.outbox.OutboxEventStatus#PENDING}
     * ja disponiveis ({@code availableAt <= now}), em ordem de criacao.
     */
    List<OutboxEvent> findPendingEvents(int limit, LocalDateTime now);

    /** Marca o evento como publicado, registrando o instante. */
    void markAsPublished(UUID eventId, LocalDateTime publishedAt);

    /**
     * Persiste o resultado de uma tentativa de publicacao que falhou.
     *
     * <p>O {@code event} ja chega com o novo estado decidido pelo dominio
     * ({@code attempts} incrementado, {@code lastError} curto, nova
     * {@code availableAt} e status {@code PENDING} para retry ou {@code FAILED}
     * ao esgotar as tentativas). O adapter apenas persiste esse estado, mantendo
     * a politica de tentativas fora da camada de persistencia.
     */
    void markAsFailed(OutboxEvent event);
}
