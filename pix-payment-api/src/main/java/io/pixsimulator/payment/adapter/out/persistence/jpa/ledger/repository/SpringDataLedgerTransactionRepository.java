package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.repository;

import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link LedgerTransactionEntity}.
 *
 * <p>As queries derivadas sao traduzidas automaticamente. O {@code operationType}
 * e comparado como {@code String} (nome do enum), coerente com a coluna
 * {@code operation_type}.
 *
 * <p>Detalhe de infraestrutura: o caso de uso depende apenas da porta
 * {@code LedgerRepository}, implementada pelo {@code JpaLedgerRepositoryAdapter}.
 */
public interface SpringDataLedgerTransactionRepository extends JpaRepository<LedgerTransactionEntity, UUID> {

    Optional<LedgerTransactionEntity> findByPaymentIdAndOperationType(UUID paymentId, String operationType);

    List<LedgerTransactionEntity> findByPaymentId(UUID paymentId);
}
