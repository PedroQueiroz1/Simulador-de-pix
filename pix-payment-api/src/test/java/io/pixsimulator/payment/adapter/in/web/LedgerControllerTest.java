package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;
import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult.LedgerEntryView;
import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult.LedgerTransactionView;
import io.pixsimulator.payment.application.exception.PaymentNotFoundException;
import io.pixsimulator.payment.application.port.in.GetLedgerByPaymentUseCase;
import io.pixsimulator.payment.domain.ledger.LedgerEntryDirection;
import io.pixsimulator.payment.domain.ledger.LedgerOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

    private static final UUID PAYMENT_ID = UUID.fromString("0197a0e8-6e3f-7e2f-b3a5-bce754c21a19");
    private static final UUID TX_ID = UUID.fromString("0197a0e9-67b7-7a2d-b870-f0ef4f11662f");
    private static final UUID DEBIT_ID = UUID.fromString("0197a0e9-6a20-7fd6-903e-b12560db94a1");
    private static final UUID CREDIT_ID = UUID.fromString("0197a0e9-6bf9-7bec-ae34-7bd01e68f1c4");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 6, 6, 10, 31, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetLedgerByPaymentUseCase getLedgerByPaymentUseCase;

    private GetLedgerByPaymentResult resultWithLedger() {
        LedgerEntryView debit = new LedgerEntryView(
                DEBIT_ID, LedgerEntryDirection.DEBIT, "11111111111", new BigDecimal("150.75"), AT);
        LedgerEntryView credit = new LedgerEntryView(
                CREDIT_ID, LedgerEntryDirection.CREDIT, "22222222222", new BigDecimal("150.75"), AT);
        LedgerTransactionView tx = new LedgerTransactionView(
                TX_ID, LedgerOperationType.PIX_SETTLEMENT, AT, List.of(debit, credit));
        return new GetLedgerByPaymentResult(PAYMENT_ID, List.of(tx));
    }

    @Test
    @DisplayName("GET ledger deve retornar 200 com entries quando ledger existe")
    void shouldReturn200WithEntriesWhenLedgerExists() throws Exception {
        when(getLedgerByPaymentUseCase.getByPaymentId(PAYMENT_ID)).thenReturn(resultWithLedger());

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}/ledger", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.transactions[0].ledgerTransactionId").value(TX_ID.toString()))
                .andExpect(jsonPath("$.transactions[0].operationType").value("PIX_SETTLEMENT"))
                .andExpect(jsonPath("$.transactions[0].entries[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.transactions[0].entries[0].accountKey").value("11111111111"))
                .andExpect(jsonPath("$.transactions[0].entries[0].amount").value(150.75))
                .andExpect(jsonPath("$.transactions[0].entries[1].direction").value("CREDIT"))
                .andExpect(jsonPath("$.transactions[0].entries[1].accountKey").value("22222222222"));
    }

    @Test
    @DisplayName("GET ledger deve retornar 200 com lista vazia quando payment existe mas nao tem ledger")
    void shouldReturn200WithEmptyListWhenNoLedger() throws Exception {
        when(getLedgerByPaymentUseCase.getByPaymentId(PAYMENT_ID))
                .thenReturn(new GetLedgerByPaymentResult(PAYMENT_ID, List.of()));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}/ledger", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions").isEmpty());
    }

    @Test
    @DisplayName("GET ledger deve retornar 404 quando payment nao existe")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        when(getLedgerByPaymentUseCase.getByPaymentId(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}/ledger", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found"))
                .andExpect(jsonPath("$.errors[0]").value("No payment was found for the provided paymentId"));
    }

    @Test
    @DisplayName("GET ledger deve retornar 400 quando paymentId e invalido")
    void shouldReturn400WhenPaymentIdInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/pix/payments/{paymentId}/ledger", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
