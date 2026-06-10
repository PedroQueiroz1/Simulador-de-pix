/* =============================================================================
 * Lote 10 — Frontend demo. JavaScript puro, sem framework.
 *
 * Mostra os fluxos principais da pix-payment-api: criar, consultar, processar e
 * consultar o ledger de um pagamento Pix.
 *
 * API base URL (configurável):
 *   - window.API_BASE_URL é definido em config.js (gerado a partir da variável
 *     de ambiente API_BASE_URL no Docker Compose);
 *   - se vazio, usa a mesma origem do frontend. No Compose, o nginx faz proxy de
 *     /api e /actuator para a pix-payment-api, então "mesma origem" funciona sem
 *     CORS. Para apontar direto a uma API remota, defina window.API_BASE_URL
 *     (essa API precisaria liberar CORS).
 * ========================================================================== */

const API_BASE_URL =
  (typeof window.API_BASE_URL === "string" && window.API_BASE_URL.trim())
    ? window.API_BASE_URL.trim().replace(/\/+$/, "")
    : "";

// ---------------------------------------------------------------------------
// Utilitários
// ---------------------------------------------------------------------------

function uuid() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    return window.crypto.randomUUID();
  }
  // Fallback simples (suficiente para uma demo).
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function $(id) {
  return document.getElementById(id);
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function statusBadge(status) {
  if (!status) return "";
  return `<span class="status-badge status-${escapeHtml(status)}">${escapeHtml(status)}</span>`;
}

function prettyJson(obj) {
  return `<pre>${escapeHtml(JSON.stringify(obj, null, 2))}</pre>`;
}

function httpLine(method, path, ok, statusCode) {
  const cls = ok ? "http-ok" : "http-err";
  return `<div class="http-line"><strong>${method}</strong> ${escapeHtml(path)} — <span class="${cls}">HTTP ${statusCode}</span></div>`;
}

function setBusy(button, busy) {
  if (!button) return;
  button.disabled = busy;
}

function requirePaymentId() {
  const id = $("paymentId").value.trim();
  if (!id) {
    throw new Error("Informe um paymentId (crie um Pix primeiro).");
  }
  return id;
}

/**
 * Faz a chamada HTTP e devolve { ok, statusCode, body }.
 * body é o JSON parseado (ou texto, se não for JSON).
 */
async function callApi(method, path, { headers = {}, body } = {}) {
  const url = `${API_BASE_URL}${path}`;
  const init = { method, headers: { ...headers } };
  if (body !== undefined) {
    init.headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(body);
  }

  const response = await fetch(url, init);
  const text = await response.text();
  let parsed = null;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch (_e) {
      parsed = text;
    }
  }
  return { ok: response.ok, statusCode: response.status, body: parsed, path };
}

/** Renderiza o corpo de erro padronizado da API (ErrorResponse) de forma legível. */
function renderError(target, method, result) {
  const body = result.body;
  let details = "";
  if (body && typeof body === "object") {
    const lines = [];
    if (body.message) lines.push(escapeHtml(body.message));
    if (Array.isArray(body.errors) && body.errors.length) {
      lines.push("<ul>" + body.errors.map((e) => `<li>${escapeHtml(e)}</li>`).join("") + "</ul>");
    }
    if (body.correlationId) {
      lines.push(`<span class="muted">correlationId: ${escapeHtml(body.correlationId)}</span>`);
    }
    details = lines.join("<br/>");
  } else if (body) {
    details = escapeHtml(body);
  }
  target.innerHTML =
    httpLine(method, result.path, false, result.statusCode) +
    `<div class="error-box">${details || "Erro na requisição."}</div>`;
}

/** Renderiza uma exceção de rede (fetch falhou — API fora do ar, CORS, etc.). */
function renderNetworkError(target, method, path, err) {
  target.innerHTML =
    `<div class="http-line"><strong>${method}</strong> ${escapeHtml(path)} — <span class="http-err">falha de rede</span></div>` +
    `<div class="error-box">${escapeHtml(err.message)}<br/><span class="muted">A API está no ar e acessível em ${escapeHtml(API_BASE_URL || "mesma origem")}?</span></div>`;
}

// ---------------------------------------------------------------------------
// Ações
// ---------------------------------------------------------------------------

async function createPayment(event) {
  event.preventDefault();
  const target = $("createResult");
  const button = event.submitter;
  const method = "POST";
  const path = "/api/v1/pix/payments";

  const headers = { "Idempotency-Key": $("idempotencyKey").value.trim() };
  const correlationId = $("correlationId").value.trim();
  if (correlationId) headers["X-Correlation-Id"] = correlationId;

  const body = {
    payerKey: $("payerKey").value.trim(),
    receiverKey: $("receiverKey").value.trim(),
    amount: Number($("amount").value),
    description: $("description").value.trim(),
  };

  try {
    setBusy(button, true);
    const result = await callApi(method, path, { headers, body });
    if (!result.ok) {
      renderError(target, method, result);
      return;
    }
    const payment = result.body;
    $("paymentId").value = payment.paymentId; // alimenta as próximas ações
    target.innerHTML =
      httpLine(method, path, true, result.statusCode) +
      statusBadge(payment.status) +
      prettyJson(payment);
    // Gera uma nova Idempotency-Key para a próxima criação não colidir.
    $("idempotencyKey").value = uuid();
  } catch (err) {
    renderNetworkError(target, method, path, err);
  } finally {
    setBusy(button, false);
  }
}

async function getPayment(event) {
  const target = $("getResult");
  const button = event.currentTarget;
  let id;
  try {
    id = requirePaymentId();
  } catch (e) {
    target.innerHTML = `<div class="error-box">${escapeHtml(e.message)}</div>`;
    return;
  }
  const method = "GET";
  const path = `/api/v1/pix/payments/${id}`;
  try {
    setBusy(button, true);
    const result = await callApi(method, path);
    if (!result.ok) {
      renderError(target, method, result);
      return;
    }
    target.innerHTML =
      httpLine(method, path, true, result.statusCode) +
      statusBadge(result.body.status) +
      prettyJson(result.body);
  } catch (err) {
    renderNetworkError(target, method, path, err);
  } finally {
    setBusy(button, false);
  }
}

async function processPayment(event) {
  const target = $("processResult");
  const button = event.currentTarget;
  let id;
  try {
    id = requirePaymentId();
  } catch (e) {
    target.innerHTML = `<div class="error-box">${escapeHtml(e.message)}</div>`;
    return;
  }
  const method = "POST";
  const path = `/api/v1/pix/payments/${id}/process`;
  try {
    setBusy(button, true);
    const result = await callApi(method, path);
    if (!result.ok) {
      renderError(target, method, result);
      return;
    }
    target.innerHTML =
      httpLine(method, path, true, result.statusCode) +
      statusBadge(result.body.status) +
      prettyJson(result.body);
  } catch (err) {
    renderNetworkError(target, method, path, err);
  } finally {
    setBusy(button, false);
  }
}

async function getLedger(event) {
  const target = $("ledgerResult");
  const button = event.currentTarget;
  let id;
  try {
    id = requirePaymentId();
  } catch (e) {
    target.innerHTML = `<div class="error-box">${escapeHtml(e.message)}</div>`;
    return;
  }
  const method = "GET";
  const path = `/api/v1/pix/payments/${id}/ledger`;
  try {
    setBusy(button, true);
    const result = await callApi(method, path);
    if (!result.ok) {
      renderError(target, method, result);
      return;
    }
    target.innerHTML = httpLine(method, path, true, result.statusCode) + renderLedger(result.body);
  } catch (err) {
    renderNetworkError(target, method, path, err);
  } finally {
    setBusy(button, false);
  }
}

/** Monta uma visão amigável do ledger (transações + entradas DEBIT/CREDIT). */
function renderLedger(ledger) {
  const transactions = (ledger && ledger.transactions) || [];
  if (transactions.length === 0) {
    return `<div class="muted">Sem ledger para este pagamento (esperado para REJECTED ou ainda não processado).</div>`;
  }
  const blocks = transactions.map((tx) => {
    const entries = (tx.entries || [])
      .map(
        (e) =>
          `<div class="entry-line"><span class="dir-${escapeHtml(e.direction)}">${escapeHtml(e.direction)}</span>` +
          `<span>${escapeHtml(e.accountKey)}</span><span>${escapeHtml(e.amount)}</span></div>`
      )
      .join("");
    return (
      `<div style="margin-bottom:0.6rem;">` +
      `<div class="muted">${escapeHtml(tx.operationType)} — ${escapeHtml(tx.ledgerTransactionId)}</div>` +
      entries +
      `</div>`
    );
  });
  return blocks.join("");
}

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------

document.addEventListener("DOMContentLoaded", () => {
  // Mostra a URL base efetiva.
  $("apiBaseUrlLabel").textContent = API_BASE_URL || "(mesma origem)";

  // Valores iniciais para Idempotency-Key e X-Correlation-Id.
  $("idempotencyKey").value = uuid();
  $("correlationId").value = uuid();

  // Botões "novo" geram um UUID no campo alvo.
  document.querySelectorAll("[data-uuid-target]").forEach((btn) => {
    btn.addEventListener("click", () => {
      $(btn.getAttribute("data-uuid-target")).value = uuid();
    });
  });

  $("createForm").addEventListener("submit", createPayment);
  $("getBtn").addEventListener("click", getPayment);
  $("processBtn").addEventListener("click", processPayment);
  $("ledgerBtn").addEventListener("click", getLedger);
});
