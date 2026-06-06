package io.pixsimulator.payment.adapter.out.persistence.jpa.repository;

import io.pixsimulator.payment.adapter.out.persistence.jpa.entity.PixPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link PixPaymentEntity}.
 *
 * O Spring gera a implementacao em tempo de execucao. A query derivada
 * {@code findByIdempotencyKey} e traduzida automaticamente para um
 * {@code WHERE idempotency_key = ?}.
 *
 * <p>Este e um detalhe de infraestrutura: o caso de uso depende apenas da porta
 * {@code PixPaymentRepository}, implementada pelo
 * {@code JpaPixPaymentRepositoryAdapter}.
 */
public interface SpringDataPixPaymentRepository extends JpaRepository<PixPaymentEntity, UUID> {

    Optional<PixPaymentEntity> findByIdempotencyKey(String idempotencyKey);
}
