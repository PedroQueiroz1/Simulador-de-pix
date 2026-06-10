package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.GetPixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
import io.pixsimulator.payment.observability.MdcKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do formato padronizado de erro (Lote 8): toda resposta de erro carrega
 * {@code correlationId} (do MDC, via {@link CorrelationIdFilter}), devolve o
 * header {@code X-Correlation-Id} e nunca expoe stack trace.
 */
@WebMvcTest(PixPaymentController.class)
class ErrorResponseCorrelationTest {

    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_VALUE = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePixPaymentUseCase createPixPaymentUseCase;

    @MockBean
    private GetPixPaymentUseCase getPixPaymentUseCase;

    @MockBean
    private ProcessPixPaymentUseCase processPixPaymentUseCase;

    @Test
    @DisplayName("Erro de validacao (400) contem correlationId e devolve o header X-Correlation-Id")
    void validationErrorContainsCorrelationId() throws Exception {
        String invalidAmountBody = """
                {
                  "payerKey": "11111111111",
                  "receiverKey": "22222222222",
                  "amount": 0,
                  "description": "Pagamento de teste"
                }
                """;

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAmountBody))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(MdcKeys.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/pix/payments"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Erro 404 contem correlationId")
    void notFoundContainsCorrelationId() throws Exception {
        when(getPixPaymentUseCase.getById(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(MdcKeys.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Erro 409 contem correlationId")
    void conflictContainsCorrelationId() throws Exception {
        when(processPixPaymentUseCase.process(PAYMENT_ID))
                .thenThrow(new PaymentNotProcessableException());

        mockMvc.perform(post("/api/v1/pix/payments/{paymentId}/process", PAYMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(header().exists(MdcKeys.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Quando o cliente envia X-Correlation-Id, o erro ecoa o mesmo valor no corpo e no header")
    void echoesProvidedCorrelationIdOnError() throws Exception {
        when(getPixPaymentUseCase.getById(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", PAYMENT_ID)
                        .header(MdcKeys.CORRELATION_ID_HEADER, "corr-from-client"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(MdcKeys.CORRELATION_ID_HEADER, "corr-from-client"))
                .andExpect(jsonPath("$.correlationId").value("corr-from-client"));
    }

    @Test
    @DisplayName("A resposta de erro nao deve expor stack trace")
    void errorDoesNotExposeStackTrace() throws Exception {
        when(getPixPaymentUseCase.getById(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString(".java"))));
    }
}
