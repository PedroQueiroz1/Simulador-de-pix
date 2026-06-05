package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.domain.exception.DomainException;
import io.pixsimulator.payment.domain.model.PixPaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PixPaymentController.class)
class PixPaymentControllerTest {

    private static final UUID PAYMENT_ID = UUID.fromString("01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41");
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_VALUE = "7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321";

    private static final String VALID_BODY = """
            {
              "payerKey": "11111111111",
              "receiverKey": "22222222222",
              "amount": 150.75,
              "description": "Pagamento de teste"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePixPaymentUseCase createPixPaymentUseCase;

    private CreatePixPaymentResult validResult() {
        return new CreatePixPaymentResult(
                PAYMENT_ID,
                PixPaymentStatus.CREATED,
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste"
        );
    }

    @Test
    @DisplayName("Deve retornar HTTP 201 para request valido")
    void shouldReturn201ForValidRequest() throws Exception {
        when(createPixPaymentUseCase.create(any())).thenReturn(validResult());

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve retornar body com paymentId, status, payerKey, receiverKey, amount e description")
    void shouldReturnBodyWithAllPublicFields() throws Exception {
        when(createPixPaymentUseCase.create(any())).thenReturn(validResult());

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.payerKey").value("11111111111"))
                .andExpect(jsonPath("$.receiverKey").value("22222222222"))
                .andExpect(jsonPath("$.amount").value(150.75))
                .andExpect(jsonPath("$.description").value("Pagamento de teste"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 para amount invalido")
    void shouldReturn400ForInvalidAmount() throws Exception {
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
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 para campos obrigatorios ausentes")
    void shouldReturn400ForMissingRequiredFields() throws Exception {
        String missingFieldsBody = """
                {
                  "description": "Pagamento sem chaves nem valor"
                }
                """;

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingFieldsBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando payerKey e receiverKey forem iguais")
    void shouldReturn400WhenPayerEqualsReceiver() throws Exception {
        when(createPixPaymentUseCase.create(any()))
                .thenThrow(new DomainException("payerKey nao pode ser igual a receiverKey"));

        String sameKeysBody = """
                {
                  "payerKey": "11111111111",
                  "receiverKey": "11111111111",
                  "amount": 150.75,
                  "description": "Pagamento de teste"
                }
                """;

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameKeysBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando header Idempotency-Key estiver ausente")
    void shouldReturn400WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A resposta nao deve expor stack trace")
    void shouldNotExposeStackTrace() throws Exception {
        when(createPixPaymentUseCase.create(any()))
                .thenThrow(new DomainException("payerKey nao pode ser igual a receiverKey"));

        String sameKeysBody = """
                {
                  "payerKey": "11111111111",
                  "receiverKey": "11111111111",
                  "amount": 150.75,
                  "description": "Pagamento de teste"
                }
                """;

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameKeysBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString(".java"))));
    }

    @Test
    @DisplayName("A resposta nao deve retornar idempotencyKey")
    void shouldNotReturnIdempotencyKey() throws Exception {
        when(createPixPaymentUseCase.create(any())).thenReturn(validResult());

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist());
    }
}
