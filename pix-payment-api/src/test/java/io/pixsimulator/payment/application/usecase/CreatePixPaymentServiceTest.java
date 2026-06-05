package io.pixsimulator.payment.application.usecase;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.port.out.IdGenerator;
import io.pixsimulator.payment.application.port.out.PixPaymentRepository;
import io.pixsimulator.payment.domain.exception.DomainException;
import io.pixsimulator.payment.domain.model.PixPayment;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePixPaymentServiceTest {

    private static final UUID FIXED_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");

    @Mock
    private PixPaymentRepository repository;

    /** Gerador de ID deterministico para testes (substitui o UUIDv7 real). */
    private final IdGenerator idGenerator = () -> FIXED_ID;

    private CreatePixPaymentCommand validCommand() {
        return new CreatePixPaymentCommand(
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321"
        );
    }

    @Test
    @DisplayName("Deve gerar UUID por meio do IdGenerator")
    void shouldGenerateIdUsingIdGenerator() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePixPaymentService service = new CreatePixPaymentService(repository, idGenerator);

        CreatePixPaymentResult result = service.create(validCommand());

        assertEquals(FIXED_ID, result.paymentId());
    }

    @Test
    @DisplayName("Deve criar o pagamento e salvar via repository")
    void shouldCreateAndSaveViaRepository() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePixPaymentService service = new CreatePixPaymentService(repository, idGenerator);

        service.create(validCommand());

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());

        PixPayment saved = captor.getValue();
        assertEquals(FIXED_ID, saved.getId());
        assertEquals("11111111111", saved.getPayerKey());
        assertEquals("22222222222", saved.getReceiverKey());
        assertEquals(new BigDecimal("150.75"), saved.getAmount());
        assertEquals("7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321", saved.getIdempotencyKey());
    }

    @Test
    @DisplayName("Deve retornar status CREATED")
    void shouldReturnCreatedStatus() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePixPaymentService service = new CreatePixPaymentService(repository, idGenerator);

        CreatePixPaymentResult result = service.create(validCommand());

        assertEquals(PixPaymentStatus.CREATED, result.status());
    }

    @Test
    @DisplayName("Deve retornar o mesmo paymentId salvo")
    void shouldReturnSamePaymentIdThatWasSaved() {
        when(repository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePixPaymentService service = new CreatePixPaymentService(repository, idGenerator);

        CreatePixPaymentResult result = service.create(validCommand());

        ArgumentCaptor<PixPayment> captor = ArgumentCaptor.forClass(PixPayment.class);
        verify(repository).save(captor.capture());

        assertEquals(captor.getValue().getId(), result.paymentId());
    }

    @Test
    @DisplayName("Deve propagar DomainException para regra violada")
    void shouldPropagateDomainExceptionWhenRuleViolated() {
        CreatePixPaymentService service = new CreatePixPaymentService(repository, idGenerator);

        CreatePixPaymentCommand invalid = new CreatePixPaymentCommand(
                "11111111111",
                "11111111111", // payer == receiver: viola regra de dominio
                new BigDecimal("150.75"),
                "Pagamento invalido",
                "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321"
        );

        assertThrows(DomainException.class, () -> service.create(invalid));
        verify(repository, never()).save(any(PixPayment.class));
    }
}
