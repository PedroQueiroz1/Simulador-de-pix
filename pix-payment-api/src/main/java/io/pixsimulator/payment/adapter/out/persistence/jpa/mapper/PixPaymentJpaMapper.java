package io.pixsimulator.payment.adapter.out.persistence.jpa.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.entity.PixPaymentEntity;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;

/**
 * Mapper manual entre o dominio {@link PixPayment} e a entidade JPA
 * {@link PixPaymentEntity}.
 *
 * E intencionalmente explicito (sem MapStruct neste lote) para deixar claro
 * onde termina o dominio e onde comeca a persistencia. O mapper NAO cria regras
 * novas: apenas converte estruturas. A unica traducao de tipo e o
 * {@link PixPaymentStatus} (enum no dominio &harr; {@code String} na entity).
 */
public final class PixPaymentJpaMapper {

    private PixPaymentJpaMapper() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /** Converte o dominio para a entidade JPA (caminho de escrita). */
    public static PixPaymentEntity toEntity(PixPayment payment) {
        return new PixPaymentEntity(
                payment.getId(),
                payment.getPayerKey(),
                payment.getReceiverKey(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getIdempotencyKey(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getProcessedAt(),
                payment.getRejectionReason()
        );
    }

    /** Reconstroi o dominio a partir da entidade JPA (caminho de leitura). */
    public static PixPayment toDomain(PixPaymentEntity entity) {
        return PixPayment.restore(
                entity.getId(),
                entity.getPayerKey(),
                entity.getReceiverKey(),
                entity.getAmount(),
                entity.getDescription(),
                entity.getIdempotencyKey(),
                PixPaymentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getProcessedAt(),
                entity.getRejectionReason()
        );
    }
}
