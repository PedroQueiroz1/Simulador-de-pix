/* =============================================================================
 * Lote 10 — Configuração da URL base da API (runtime).
 *
 * Vazio ("") = mesma origem do frontend. No Docker Compose, o nginx faz proxy
 * de /api e /actuator para a pix-payment-api, então o padrão funciona sem CORS.
 *
 * No container, este arquivo é REGERADO na subida a partir da variável de
 * ambiente API_BASE_URL (ver Dockerfile). Para uso fora do Docker, edite o valor
 * abaixo apontando para uma API acessível (que precisaria liberar CORS).
 * NUNCA coloque segredos aqui — é servido publicamente.
 * ========================================================================== */
window.API_BASE_URL = "";
