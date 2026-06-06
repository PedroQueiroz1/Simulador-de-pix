package io.pixsimulator.payment.adapter.out.persistence.jpa;

import io.pixsimulator.payment.adapter.out.persistence.jpa.entity.PixPaymentEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.mapper.PixPaymentJpaMapper;
import io.pixsimulator.payment.adapter.out.persistence.jpa.repository.SpringDataPixPaymentRepository;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saida que implementa a porta {@link PixPaymentRepository} usando
 * Spring Data JPA + SQL Server.
 *
 * Traduz dominio &harr; entity via {@link PixPaymentJpaMapper} e
 * delega o acesso ao banco ao {@link SpringDataPixPaymentRepository}.
 *
 * A transacao e controlada aqui, no adapter, por serem operacoes de
 * persistencia simples neste lote. Em lotes futuros (payment + ledger + outbox)
 * a transacao devera subir para o nivel do caso de uso.
 */
@Repository
public class JpaPixPaymentRepositoryAdapter implements PixPaymentRepository {

    private final SpringDataPixPaymentRepository jpaRepository;

    public JpaPixPaymentRepositoryAdapter(SpringDataPixPaymentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public PixPayment save(PixPayment payment) {
        PixPaymentEntity entity = PixPaymentJpaMapper.toEntity(payment);
        // saveAndFlush forca o INSERT imediatamente, fazendo a constraint unica
        // de idempotency_key falhar na hora (DataIntegrityViolationException),
        // que e a ultima barreira contra duplicidade concorrente.
        PixPaymentEntity saved = jpaRepository.saveAndFlush(entity);
        return PixPaymentJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PixPayment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(PixPaymentJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PixPayment> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId)
                .map(PixPaymentJpaMapper::toDomain);
    }
}
