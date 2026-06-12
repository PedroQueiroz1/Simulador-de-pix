package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger;

import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerTransactionEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.mapper.LedgerJpaMapper;
import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.repository.SpringDataLedgerTransactionRepository;
import io.pixsimulator.payment.application.port.out.LedgerRepository;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saida que implementa a porta {@link LedgerRepository} usando
 * Spring Data JPA + SQL Server.
 *
 * <p>Traduz dominio &harr; entity via {@link LedgerJpaMapper} e delega o acesso
 * ao banco ao {@link SpringDataLedgerTransactionRepository}.
 *
 * <p>As operacoes usam {@code @Transactional} com propagacao padrao
 * ({@code REQUIRED}): quando chamadas dentro do processamento de pagamento,
 * participam da transacao aberta no caso de uso, garantindo que a aprovacao do
 * pagamento e a criacao do Ledger sejam atomicas.
 */
@Repository
public class JpaLedgerRepositoryAdapter implements LedgerRepository {

    private final SpringDataLedgerTransactionRepository jpaRepository;

    public JpaLedgerRepositoryAdapter(SpringDataLedgerTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public LedgerTransaction save(LedgerTransaction ledgerTransaction) {
        LedgerTransactionEntity entity = LedgerJpaMapper.toEntity(ledgerTransaction);
        // saveAndFlush forca o INSERT imediatamente: a constraint unica
        // (payment_id, operation_type) falha na hora (DataIntegrityViolationException),
        // ultima barreira contra settlement duplicado.
        LedgerTransactionEntity saved = jpaRepository.saveAndFlush(entity);
        return LedgerJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LedgerTransaction> findByPaymentIdAndOperationType(UUID paymentId,
                                                                       LedgerOperationType operationType) {
        return jpaRepository.findByPaymentIdAndOperationType(paymentId, operationType.name())
                .map(LedgerJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerTransaction> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).stream()
                .map(LedgerJpaMapper::toDomain)
                .toList();
    }
}
