package io.pixsimulator.payment.application.idempotency;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Testes do gerador de fingerprint do payload.
 *
 * <p>Garante que payloads equivalentes (mesma intencao de pagamento) gerem o
 * mesmo hash, mesmo com escala/espacos diferentes, e que qualquer mudanca real
 * de conteudo gere hash diferente.
 */
class RequestFingerprintGeneratorTest {

    private final RequestFingerprintGenerator generator = new RequestFingerprintGenerator();

    private CreatePixPaymentCommand command(String payerKey,
                                            String receiverKey,
                                            BigDecimal amount,
                                            String description) {
        // A idempotencyKey nao entra no fingerprint; usamos um valor fixo.
        return new CreatePixPaymentCommand(payerKey, receiverKey, amount, description, "idem-key");
    }

    @Test
    @DisplayName("Mesmo payload deve gerar o mesmo hash")
    void samePayloadSameHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento");
        CreatePixPaymentCommand b = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento");

        assertEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("Amount equivalente com escala diferente deve gerar o mesmo hash")
    void equivalentAmountDifferentScaleSameHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.7"), "Pagamento");
        CreatePixPaymentCommand b = command("11111111111", "22222222222", new BigDecimal("150.700"), "Pagamento");

        assertEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("Espacos extras nas strings devem gerar o mesmo hash")
    void extraWhitespaceSameHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento");
        CreatePixPaymentCommand b = command("  11111111111  ", " 22222222222 ", new BigDecimal("150.75"), "  Pagamento  ");

        assertEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("Amount diferente deve gerar hash diferente")
    void differentAmountDifferentHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento");
        CreatePixPaymentCommand b = command("11111111111", "22222222222", new BigDecimal("999.00"), "Pagamento");

        assertNotEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("ReceiverKey diferente deve gerar hash diferente")
    void differentReceiverKeyDifferentHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento");
        CreatePixPaymentCommand b = command("11111111111", "33333333333", new BigDecimal("150.75"), "Pagamento");

        assertNotEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("Description diferente deve gerar hash diferente")
    void differentDescriptionDifferentHash() {
        CreatePixPaymentCommand a = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento A");
        CreatePixPaymentCommand b = command("11111111111", "22222222222", new BigDecimal("150.75"), "Pagamento B");

        assertNotEquals(generator.generate(a), generator.generate(b));
    }

    @Test
    @DisplayName("Description nula deve ser tratada como string vazia (nao quebra)")
    void nullDescriptionTreatedAsEmpty() {
        CreatePixPaymentCommand withNull = command("11111111111", "22222222222", new BigDecimal("150.75"), null);
        CreatePixPaymentCommand withEmpty = command("11111111111", "22222222222", new BigDecimal("150.75"), "");

        assertEquals(generator.generate(withNull), generator.generate(withEmpty));
    }
}
