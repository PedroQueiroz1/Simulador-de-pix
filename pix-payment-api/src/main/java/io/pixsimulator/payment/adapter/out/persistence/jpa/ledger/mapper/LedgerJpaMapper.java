package io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerEntryEntity;
import io.pixsimulator.payment.adapter.out.persistence.jpa.ledger.entity.LedgerTransactionEntity;
import io.pixsimulator.payment.domain.ledger.LedgerEntry;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import io.pixsimulator.payment.domain.ledger.LedgerTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper manual entre o dominio do Ledger ({@link LedgerTransaction} /
 * {@link LedgerEntry}) e as entidades JPA ({@link LedgerTransactionEntity} /
 * {@link LedgerEntryEntity}).
 *
 * <p>Intencionalmente explicito (sem MapStruct), deixando claro a fronteira
 * entre dominio e persistencia. Nao cria regras novas: apenas converte
 * estruturas. As unicas traducoes de tipo sao os enums
 * ({@link LedgerOperationType} e {@link LedgerEntryDirection}) &harr;
 * {@code String}.
 */
public final class LedgerJpaMapper {

    private LedgerJpaMapper() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /** Converte o dominio para a entidade JPA (caminho de escrita). */
    public static LedgerTransactionEntity toEntity(LedgerTransaction transaction) {
        List<LedgerEntryEntity> entryEntities = new ArrayList<>();
        for (LedgerEntry entry : transaction.getEntries()) {
            entryEntities.add(toEntity(entry));
        }

        return new LedgerTransactionEntity(
                transaction.getId(),
                transaction.getPaymentId(),
                transaction.getOperationType().name(),
                transaction.getCreatedAt(),
                entryEntities
        );
    }

    /** Converte uma entry de dominio para a entidade JPA. */
    public static LedgerEntryEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(
                entry.getId(),
                entry.getLedgerTransactionId(),
                entry.getPaymentId(),
                entry.getAccountKey(),
                entry.getDirection().name(),
                entry.getAmount(),
                entry.getCreatedAt()
        );
    }

    /** Reconstroi o dominio a partir da entidade JPA (caminho de leitura). */
    public static LedgerTransaction toDomain(LedgerTransactionEntity entity) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (LedgerEntryEntity entryEntity : entity.getEntries()) {
            entries.add(toDomain(entryEntity));
        }

        return LedgerTransaction.restore(
                entity.getId(),
                entity.getPaymentId(),
                LedgerOperationType.valueOf(entity.getOperationType()),
                entries,
                entity.getCreatedAt()
        );
    }

    /** Reconstroi uma entry de dominio a partir da entidade JPA. */
    public static LedgerEntry toDomain(LedgerEntryEntity entity) {
        return LedgerEntry.restore(
                entity.getId(),
                entity.getLedgerTransactionId(),
                entity.getPaymentId(),
                entity.getAccountKey(),
                LedgerEntryDirection.valueOf(entity.getDirection()),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }
}
