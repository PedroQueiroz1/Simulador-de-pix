package io.pixsimulator.payment.adapter.out.persistence.inmemory;

import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de saida que persiste pagamentos em memoria. (LEGADO)
 * O bean ativo de persistencia passou a ser o {@code JpaPixPaymentRepositoryAdapter}
 * (SQL Server). A classe permanece no codigo como implementacao alternativa da
 * porta {@link PixPaymentRepository}, util para testes leves e como referencia
 * de que o caso de uso nao depende da tecnologia concreta.
 */
public class InMemoryPixPaymentRepository implements PixPaymentRepository {

    private final ConcurrentHashMap<UUID, PixPayment> storage = new ConcurrentHashMap<>();

    @Override
    public PixPayment save(PixPayment payment) {
        storage.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<PixPayment> findByIdempotencyKey(String idempotencyKey) {
        return storage.values().stream()
                .filter(payment -> payment.getIdempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public Optional<PixPayment> findById(UUID paymentId) {
        return Optional.ofNullable(storage.get(paymentId));
    }
}
