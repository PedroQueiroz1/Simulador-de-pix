package io.pixsimulator.payment.adapter.out.persistence.inmemory;

import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.model.PixPayment;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de saida que persiste pagamentos em memoria.
 *
 * <p>Implementacao temporaria do Lote 1, usando {@link ConcurrentHashMap} para
 * suportar acesso concorrente. Sera substituida por SQL Server no Lote 2 sem
 * impacto no caso de uso, que depende apenas da porta
 * {@link PixPaymentRepository}.
 */
@Repository
public class InMemoryPixPaymentRepository implements PixPaymentRepository {

    private final ConcurrentHashMap<UUID, PixPayment> storage = new ConcurrentHashMap<>();

    @Override
    public PixPayment save(PixPayment payment) {
        storage.put(payment.getId(), payment);
        return payment;
    }
}
