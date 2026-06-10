package io.github.shopkdris.pixsimulator.notification.application.notification;

import io.github.shopkdris.pixsimulator.notification.application.dto.PaymentEventMessage;

/**
 * Simulador de notificacao (Lote 7, ADR-032).
 *
 * <p>Apenas <strong>monta uma mensagem</strong> de notificacao a partir do
 * evento, retornando o texto que seria enviado ao cliente. NAO chama API
 * externa, NAO envia e-mail/SMS/WhatsApp/push e NAO usa credenciais. Mantem o
 * foco do lote em Kafka, idempotencia de consumo e MongoDB, e torna o teste
 * deterministico.
 */
public class NotificationSimulator {

    /**
     * Constroi a mensagem simulada conforme o tipo do evento. O resultado e
     * gravado em {@code notification_audits.notificationMessage}.
     */
    public String simulate(PaymentEventMessage event) {
        return switch (event.eventType()) {
            case PAYMENT_CREATED -> String.format(
                    "Pix de R$ %s criado para a chave %s. Pagamento %s aguardando processamento.",
                    event.amount(), event.receiverKey(), event.paymentId());
            case PAYMENT_APPROVED -> String.format(
                    "Pix de R$ %s APROVADO para a chave %s. Pagamento %s liquidado (ledger %s).",
                    event.amount(), event.receiverKey(), event.paymentId(), event.ledgerTransactionId());
            case PAYMENT_REJECTED -> String.format(
                    "Pix de R$ %s REJEITADO para a chave %s. Pagamento %s. Motivo: %s.",
                    event.amount(), event.receiverKey(), event.paymentId(), event.rejectionReason());
        };
    }
}
