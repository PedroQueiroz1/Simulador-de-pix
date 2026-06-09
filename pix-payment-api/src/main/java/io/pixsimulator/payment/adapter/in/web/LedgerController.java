package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.application.dto.GetLedgerByPaymentResult;
import io.pixsimulator.payment.application.port.in.GetLedgerByPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adapter de entrada HTTP para consulta do Ledger (Lote 5).
 *
 * <p>Endpoint:
 * <ul>
 *   <li>{@code GET /api/v1/pix/payments/{paymentId}/ledger} — lista as transacoes
 *       de ledger do pagamento.</li>
 * </ul>
 *
 * <p>Nao existe endpoint para criar Ledger: ele nasce apenas do fluxo de
 * processamento aprovado. Comportamento:
 * <ul>
 *   <li>payment existe com ledger -&gt; 200 com transactions/entries;</li>
 *   <li>payment existe sem ledger -&gt; 200 com lista vazia;</li>
 *   <li>payment inexistente -&gt; 404 ({@code PaymentNotFoundException});</li>
 *   <li>{@code paymentId} invalido -&gt; 400 (tratado no {@code RestExceptionHandler}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pix/payments")
public class LedgerController {

    private final GetLedgerByPaymentUseCase getLedgerByPaymentUseCase;

    public LedgerController(GetLedgerByPaymentUseCase getLedgerByPaymentUseCase) {
        this.getLedgerByPaymentUseCase = getLedgerByPaymentUseCase;
    }

    @GetMapping("/{paymentId}/ledger")
    public ResponseEntity<LedgerResponse> getLedger(@PathVariable UUID paymentId) {
        GetLedgerByPaymentResult result = getLedgerByPaymentUseCase.getByPaymentId(paymentId);
        return ResponseEntity.ok(LedgerResponse.from(result));
    }
}
