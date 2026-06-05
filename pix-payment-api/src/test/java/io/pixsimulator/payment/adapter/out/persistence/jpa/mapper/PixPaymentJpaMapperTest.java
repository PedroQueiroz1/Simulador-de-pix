package io.pixsimulator.payment.adapter.out.persistence.jpa.mapper;

import io.pixsimulator.payment.adapter.out.persistence.jpa.entity.PixPaymentEntity;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixPaymentJpaMapperTest {

    private static final UUID ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String PAYER = "11111111111";
    private static final String RECEIVER = "22222222222";
    private static final BigDecimal AMOUNT = new BigDecimal("150.75");
    private static final String DESCRIPTION = "Pagamento de teste";
    private static final String IDEMPOTENCY_KEY = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 4, 10, 30, 0);

    private PixPayment domain() {
        return PixPayment.restore(
                ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY,
                PixPaymentStatus.CREATED, CREATED_AT);
    }

    private PixPaymentEntity entity() {
        return new PixPaymentEntity(
                ID, PAYER, RECEIVER, AMOUNT, DESCRIPTION, IDEMPOTENCY_KEY,
                PixPaymentStatus.CREATED.name(), CREATED_AT);
    }

    @Test
    @DisplayName("Deve converter PixPayment (dominio) para PixPaymentEntity preservando todos os campos")
    void shouldMapDomainToEntity() {
        PixPaymentEntity entity = PixPaymentJpaMapper.toEntity(domain());

        assertEquals(ID, entity.getId());
        assertEquals(PAYER, entity.getPayerKey());
        assertEquals(RECEIVER, entity.getReceiverKey());
        assertEquals(AMOUNT, entity.getAmount());
        assertEquals(DESCRIPTION, entity.getDescription());
        assertEquals(IDEMPOTENCY_KEY, entity.getIdempotencyKey());
        assertEquals("CREATED", entity.getStatus());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    @DisplayName("Deve converter PixPaymentEntity para PixPayment (dominio) preservando todos os campos")
    void shouldMapEntityToDomain() {
        PixPayment domain = PixPaymentJpaMapper.toDomain(entity());

        assertEquals(ID, domain.getId());
        assertEquals(PAYER, domain.getPayerKey());
        assertEquals(RECEIVER, domain.getReceiverKey());
        assertEquals(AMOUNT, domain.getAmount());
        assertEquals(DESCRIPTION, domain.getDescription());
        assertEquals(IDEMPOTENCY_KEY, domain.getIdempotencyKey());
        assertEquals(PixPaymentStatus.CREATED, domain.getStatus());
        assertEquals(CREATED_AT, domain.getCreatedAt());
    }

    @Test
    @DisplayName("Status deve ser persistido como String (nome do enum), nunca ordinal")
    void shouldMapStatusAsEnumName() {
        PixPaymentEntity entity = PixPaymentJpaMapper.toEntity(domain());

        assertEquals("CREATED", entity.getStatus());
    }

    @Test
    @DisplayName("Round-trip dominio -> entity -> dominio deve preservar o estado")
    void shouldRoundTripWithoutLoss() {
        PixPayment original = domain();

        PixPayment roundTrip = PixPaymentJpaMapper.toDomain(PixPaymentJpaMapper.toEntity(original));

        assertEquals(original.getId(), roundTrip.getId());
        assertEquals(original.getPayerKey(), roundTrip.getPayerKey());
        assertEquals(original.getReceiverKey(), roundTrip.getReceiverKey());
        assertEquals(original.getAmount(), roundTrip.getAmount());
        assertEquals(original.getDescription(), roundTrip.getDescription());
        assertEquals(original.getIdempotencyKey(), roundTrip.getIdempotencyKey());
        assertEquals(original.getStatus(), roundTrip.getStatus());
        assertEquals(original.getCreatedAt(), roundTrip.getCreatedAt());
    }
}
