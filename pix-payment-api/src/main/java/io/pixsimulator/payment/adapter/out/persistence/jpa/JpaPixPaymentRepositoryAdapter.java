package io.pixsimulator.payment.adapter.out.persistence.jpa;

import io.pixsimulator.payment.adapter.out.persistence.jpa.entity.PixPaymentEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.mapper.PixPaymentJpaMapper;
import io.pixsimulator.payment.adapter.out.persistence.jpa.repository.SpringDataPixPaymentRepository;
import io.pixsimulator.payment.application.exception.DuplicateIdempotencyKeyException;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import org.springframework.dao.DataIntegrityViolationException;
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
 * Tambem traduz a violacao da constraint unica de {@code idempotency_key}
 * (a ultima barreira contra duplicidade concorrente, ADR-009) em
 * {@link DuplicateIdempotencyKeyException}, para que a aplicacao recupere o
 * caso sem conhecer detalhes de banco/Spring Data.
 */
@Repository
public class JpaPixPaymentRepositoryAdapter implements PixPaymentRepository {

    /** Nome da constraint unica de idempotency_key (migration V1). */
    static final String IDEMPOTENCY_KEY_CONSTRAINT = "UK_pix_payments_idempotency_key";

    private final SpringDataPixPaymentRepository jpaRepository;

    public JpaPixPaymentRepositoryAdapter(SpringDataPixPaymentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public PixPayment save(PixPayment payment) {
        PixPaymentEntity entity = PixPaymentJpaMapper.toEntity(payment);
        try {
            // saveAndFlush forca o INSERT imediatamente, fazendo a constraint
            // unica de idempotency_key falhar na hora, ainda dentro do save.
            PixPaymentEntity saved = jpaRepository.saveAndFlush(entity);
            return PixPaymentJpaMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (isIdempotencyKeyViolation(e)) {
                throw new DuplicateIdempotencyKeyException(payment.getIdempotencyKey(), e);
            }
            throw e;
        }
    }

    /**
     * Confirma que a violacao veio da constraint de idempotency_key (e nao de
     * outra constraint), procurando o nome dela na causa raiz reportada pelo
     * SQL Server. Evita traduzir violacoes nao relacionadas.
     */
    private static boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(IDEMPOTENCY_KEY_CONSTRAINT);
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
