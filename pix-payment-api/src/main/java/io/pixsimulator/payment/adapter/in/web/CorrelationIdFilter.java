package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.observability.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro de rastreabilidade.
 *
 * <p>Responsabilidades, por requisicao:
 * <ol>
 *   <li>ler os headers {@code X-Correlation-Id} e {@code X-Request-Id};</li>
 *   <li>gerar um novo valor (UUID) para os que estiverem ausentes/em branco;</li>
 *   <li>colocar ambos no MDC ({@code correlationId}/{@code requestId}), para que
 *       apareçam nos logs de toda a requisicao;</li>
 *   <li>devolver os dois valores nos headers da response (inclusive em erros,
 *       pois os headers sao definidos antes do {@code doFilter});</li>
 *   <li>limpar o MDC ao final, evitando vazamento de contexto entre threads do
 *       pool do servidor.</li>
 * </ol>
 *
 * <p>Roda antes de tudo ({@link Ordered#HIGHEST_PRECEDENCE}) para que o
 * correlationId ja esteja no MDC quando qualquer log (ou o
 * {@code RestExceptionHandler}) for acionado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveOrGenerate(request.getHeader(MdcKeys.CORRELATION_ID_HEADER));
        String requestId = resolveOrGenerate(request.getHeader(MdcKeys.REQUEST_ID_HEADER));

        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        MDC.put(MdcKeys.REQUEST_ID, requestId);

        // Definidos antes do doFilter: garante que ate as respostas de erro
        // (geradas downstream) carreguem os headers de correlacao.
        response.setHeader(MdcKeys.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(MdcKeys.REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpa TODAS as chaves de contexto da requisicao (inclusive as que
            // os casos de uso colocam, como paymentId), evitando vazar contexto
            // para a proxima requisicao atendida pela mesma thread do pool.
            MDC.remove(MdcKeys.CORRELATION_ID);
            MDC.remove(MdcKeys.REQUEST_ID);
            MDC.remove(MdcKeys.PAYMENT_ID);
            MDC.remove(MdcKeys.OUTBOX_EVENT_ID);
            MDC.remove(MdcKeys.LEDGER_TRANSACTION_ID);
        }
    }

    private static String resolveOrGenerate(String headerValue) {
        return (headerValue != null && !headerValue.isBlank())
                ? headerValue.trim()
                : UUID.randomUUID().toString();
    }
}
