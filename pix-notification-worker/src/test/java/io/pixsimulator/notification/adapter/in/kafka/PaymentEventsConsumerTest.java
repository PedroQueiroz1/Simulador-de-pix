package io.pixsimulator.notification.adapter.in.kafka;

import io.pixsimulator.notification.application.consumer.PaymentEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Testes do {@link PaymentEventsConsumer} com {@link PaymentEventHandler}
 * mockado (Lote 7).
 *
 * <p>O consumer e um adapter fino: so delega para o handler. Os testes garantem
 * exatamente isso — a mensagem e repassada intacta e o consumer nao executa
 * nenhuma regra de negocio (nenhuma interacao alem da delegacao).
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventsConsumerTest {

    @Mock
    private PaymentEventHandler paymentEventHandler;

    @Test
    @DisplayName("Deve delegar a mensagem recebida para o PaymentEventHandler")
    void delegatesMessageToHandler() {
        PaymentEventsConsumer consumer = new PaymentEventsConsumer(paymentEventHandler);
        String payload = "{\"eventId\":\"0197a0e8-6e3f-7e2f-b3a5-bce754c21a19\"}";

        consumer.onMessage(payload);

        verify(paymentEventHandler).handle(payload);
    }

    @Test
    @DisplayName("Nao deve conter regra de negocio: apenas delega, sem outras interacoes")
    void doesNotContainBusinessLogic() {
        PaymentEventsConsumer consumer = new PaymentEventsConsumer(paymentEventHandler);
        String payload = "qualquer-conteudo";

        consumer.onMessage(payload);

        // Unica interacao permitida: repassar o payload cru ao handler.
        verify(paymentEventHandler).handle(payload);
        verifyNoMoreInteractions(paymentEventHandler);
    }
}
