package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.CreatePixPaymentResult;
import io.pixsimulator.payment.application.dto.GetPixPaymentResult;
import io.pixsimulator.payment.application.dto.ProcessPixPaymentResult;
import io.pixsimulator.payment.application.exception.IdempotencyConflictException;
import io.pixsimulator.payment.application.exception.IdempotencyInProgressException;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.exception.PaymentNotProcessableException;
import io.pixsimulator.payment.application.port.in.CreatePixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.GetPixPaymentUseCase;
import io.pixsimulator.payment.application.port.in.ProcessPixPaymentUseCase;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockBean
    private GetPixPaymentUseCase getPixPaymentUseCase;

    @MockBean
    private ProcessPixPaymentUseCase processPixPaymentUseCase;

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

    @Test
    @DisplayName("Retry equivalente deve retornar HTTP 201 com o mesmo paymentId")
    void shouldReturn201WithSamePaymentIdOnEquivalentRetry() throws Exception {
        // O caso de uso devolve a mesma resposta (mesmo paymentId) nas duas chamadas:
        // e o comportamento idempotente do retry equivalente, visto pelo controller.
        when(createPixPaymentUseCase.create(any())).thenReturn(validResult());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/pix/payments")
                            .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()));
        }
    }

    @Test
    @DisplayName("Mesma chave com payload diferente deve retornar HTTP 409 (conflito)")
    void shouldReturn409OnIdempotencyConflict() throws Exception {
        when(createPixPaymentUseCase.create(any()))
                .thenThrow(new IdempotencyConflictException(IDEMPOTENCY_VALUE));

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Idempotency conflict"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]")
                        .value("The Idempotency-Key was already used with a different request payload"));
    }

    @Test
    @DisplayName("Mesma chave em processamento deve retornar HTTP 409 (in progress)")
    void shouldReturn409WhenIdempotencyInProgress() throws Exception {
        when(createPixPaymentUseCase.create(any()))
                .thenThrow(new IdempotencyInProgressException(IDEMPOTENCY_VALUE));

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Idempotency key is already processing"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]")
                        .value("A request with this Idempotency-Key is still being processed"));
    }

    @Test
    @DisplayName("Resposta de conflito de idempotencia nao deve expor stack trace")
    void idempotencyConflictShouldNotExposeStackTrace() throws Exception {
        when(createPixPaymentUseCase.create(any()))
                .thenThrow(new IdempotencyConflictException(IDEMPOTENCY_VALUE));

        mockMvc.perform(post("/api/v1/pix/payments")
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString(".java"))));
    }

    // ----------------------------------------------------------------------
    // GET /api/v1/pix/payments/{paymentId}
    // ----------------------------------------------------------------------

    private GetPixPaymentResult getResult() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 6, 10, 30, 0);
        return new GetPixPaymentResult(
                PAYMENT_ID,
                PixPaymentStatus.CREATED,
                "11111111111",
                "22222222222",
                new BigDecimal("150.75"),
                "Pagamento de teste",
                createdAt,
                createdAt,
                null,
                null
        );
    }

    @Test
    @DisplayName("GET deve retornar HTTP 200 com o pagamento quando existir")
    void getShouldReturn200WhenPaymentExists() throws Exception {
        when(getPixPaymentUseCase.getById(PAYMENT_ID)).thenReturn(getResult());

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.payerKey").value("11111111111"))
                .andExpect(jsonPath("$.receiverKey").value("22222222222"))
                .andExpect(jsonPath("$.amount").value(150.75))
                .andExpect(jsonPath("$.description").value("Pagamento de teste"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.processedAt").doesNotExist())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    @DisplayName("GET deve retornar HTTP 404 quando o pagamento nao existir")
    void getShouldReturn404WhenPaymentNotFound() throws Exception {
        when(getPixPaymentUseCase.getById(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value("No payment was found for the provided paymentId"));
    }

    @Test
    @DisplayName("GET deve retornar HTTP 400 quando o paymentId nao for um UUID valido")
    void getShouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ----------------------------------------------------------------------
    // POST /api/v1/pix/payments/{paymentId}/process
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("POST process deve aprovar pagamento elegivel (HTTP 200, APPROVED)")
    void processShouldApproveEligiblePayment() throws Exception {
        ProcessPixPaymentResult approved = new ProcessPixPaymentResult(
                PAYMENT_ID,
                PixPaymentStatus.APPROVED,
                LocalDateTime.of(2026, 6, 6, 10, 31, 0),
                null
        );
        when(processPixPaymentUseCase.process(PAYMENT_ID)).thenReturn(approved);

        mockMvc.perform(post("/api/v1/pix/payments/{paymentId}/process", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.processedAt").exists())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    @DisplayName("POST process deve rejeitar pagamento acima do limite (HTTP 200, REJECTED)")
    void processShouldRejectPaymentAboveLimit() throws Exception {
        ProcessPixPaymentResult rejected = new ProcessPixPaymentResult(
                PAYMENT_ID,
                PixPaymentStatus.REJECTED,
                LocalDateTime.of(2026, 6, 6, 10, 31, 0),
                "Amount exceeds the simulated approval limit"
        );
        when(processPixPaymentUseCase.process(PAYMENT_ID)).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/pix/payments/{paymentId}/process", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.processedAt").exists())
                .andExpect(jsonPath("$.rejectionReason").value("Amount exceeds the simulated approval limit"));
    }

    @Test
    @DisplayName("POST process deve retornar HTTP 404 quando o pagamento nao existir")
    void processShouldReturn404WhenPaymentNotFound() throws Exception {
        when(processPixPaymentUseCase.process(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(post("/api/v1/pix/payments/{paymentId}/process", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found"))
                .andExpect(jsonPath("$.errors[0]").value("No payment was found for the provided paymentId"));
    }

    @Test
    @DisplayName("POST process deve retornar HTTP 409 quando o pagamento ja estiver em status terminal")
    void processShouldReturn409WhenPaymentIsTerminal() throws Exception {
        when(processPixPaymentUseCase.process(PAYMENT_ID))
                .thenThrow(new PaymentNotProcessableException());

        mockMvc.perform(post("/api/v1/pix/payments/{paymentId}/process", PAYMENT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment cannot be processed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value("Payment is already in a terminal status"));
    }
}
