package io.pixsimulator.payment.application.idempotency;

import io.pixsimulator.payment.application.dto.CreatePixPaymentCommand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Gera um fingerprint (hash SHA-256) deterministico do payload de uma
 * requisicao de criacao de pagamento.
 *
 * Serve para decidir se uma repeticao da mesma {@code Idempotency-Key} e de
 * fato a mesma intencao de pagamento (retry seguro) ou um reuso indevido da
 * chave com outro payload (conflito 409).
 *
 * Campos considerados: {@code payerKey}, {@code receiverKey}, {@code amount}
 * e {@code description}. Nao entram {@code paymentId}, {@code createdAt},
 * {@code status} (gerados pelo servidor) nem {@code idempotencyKey} (e a chave
 * do registro, nao o conteudo do pagamento).
 *
 * Normalizacao aplicada antes do hash para que payloads equivalentes gerem o
 * mesmo hash: {@code trim} em payerKey/receiverKey/description, description nula
 * vira string vazia e amount e normalizado para escala 2.
 *
 * Nao depende de Spring nem de Redis: e uma classe pura, instanciada na
 * configuracao de wiring.
 */
public class RequestFingerprintGenerator {

    /**
     * Gera o hash SHA-256 (hex) da forma canonica do payload do comando.
     */
    public String generate(CreatePixPaymentCommand command) {
        String canonical = canonicalForm(command);
        return sha256Hex(canonical);
    }

    private String canonicalForm(CreatePixPaymentCommand command) {
        String payerKey = normalizeText(command.payerKey());
        String receiverKey = normalizeText(command.receiverKey());
        String description = normalizeText(command.description());
        String amount = normalizeAmount(command.amount());

        return "payerKey=" + payerKey
                + "|receiverKey=" + receiverKey
                + "|amount=" + amount
                + "|description=" + description;
    }

    /** Aplica trim; trata nulo como string vazia. */
    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    /** Normaliza para escala 2 (ex.: 150.7 -> 150.70, 150.700 -> 150.70). */
    private String normalizeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e garantido pela plataforma Java; nao deve ocorrer.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
