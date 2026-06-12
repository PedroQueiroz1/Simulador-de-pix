package io.pixsimulator.payment.adapter.in.web;

import io.pixsimulator.payment.observability.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link CorrelationIdFilter}.
 *
 * <p>Exercita o filtro isoladamente com objetos mock do Spring: leitura/geracao
 * dos ids, devolucao nos headers, presenca no MDC durante a cadeia e limpeza do
 * MDC ao final (inclusive quando a cadeia lanca excecao).
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Quando X-Correlation-Id vem na request, a response mantem o mesmo valor")
    void keepsProvidedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcKeys.CORRELATION_ID_HEADER, "corr-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("corr-abc-123", response.getHeader(MdcKeys.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Quando nao vem X-Correlation-Id, a response recebe um novo correlationId (UUID)")
    void generatesCorrelationIdWhenAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(MdcKeys.CORRELATION_ID_HEADER);
        assertNotNull(generated);
        assertTrue(generated != null && !generated.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(generated), "deve ser um UUID valido");
    }

    @Test
    @DisplayName("A response sempre contem X-Request-Id (gerado quando ausente)")
    void alwaysReturnsRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(MdcKeys.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertTrue(!requestId.isBlank());
    }

    @Test
    @DisplayName("O correlationId/requestId ficam no MDC durante a cadeia e sao limpos ao final")
    void populatesMdcDuringChainAndClearsAfter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcKeys.CORRELATION_ID_HEADER, "corr-xyz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] captured = new String[2];
        FilterChain chain = (req, res) -> {
            captured[0] = MDC.get(MdcKeys.CORRELATION_ID);
            captured[1] = MDC.get(MdcKeys.REQUEST_ID);
        };

        filter.doFilter(request, response, chain);

        assertEquals("corr-xyz", captured[0]);
        assertNotNull(captured[1]);
        // MDC limpo ao final, evitando vazamento entre requisicoes.
        assertNull(MDC.get(MdcKeys.CORRELATION_ID));
        assertNull(MDC.get(MdcKeys.REQUEST_ID));
    }

    @Test
    @DisplayName("Mesmo quando a cadeia lanca erro, os headers de correlacao sao devolvidos e o MDC e limpo")
    void setsHeadersAndClearsMdcEvenOnError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcKeys.CORRELATION_ID_HEADER, "corr-on-error");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, failingChain));

        // Headers definidos antes do doFilter => presentes mesmo no erro.
        assertEquals("corr-on-error", response.getHeader(MdcKeys.CORRELATION_ID_HEADER));
        assertNotNull(response.getHeader(MdcKeys.REQUEST_ID_HEADER));
        // MDC limpo pelo finally.
        assertNull(MDC.get(MdcKeys.CORRELATION_ID));
        assertNull(MDC.get(MdcKeys.REQUEST_ID));
    }
}
