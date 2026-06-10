# =============================================================================
# Lote 10 — frontend-demo (nginx servindo HTML/CSS/JS puro)
#
# Build:
#   docker build -t pix-frontend-demo:local frontend-demo
#
# A URL base da API é configurável em runtime via variável de ambiente
# API_BASE_URL: a entrada abaixo regenera /usr/share/nginx/html/config.js na
# subida do container. Vazio = mesma origem (o nginx faz proxy de /api e
# /actuator para a pix-payment-api). NÃO há segredos nesta imagem.
# =============================================================================
FROM nginx:1.27-alpine

# Config do nginx (static + reverse-proxy de /api e /actuator).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Arquivos estáticos do frontend.
COPY index.html styles.css app.js config.js /usr/share/nginx/html/

# URL base da API (vazio = mesma origem via proxy do nginx). Sobreposta no Compose.
ENV API_BASE_URL=""

EXPOSE 80

# Regenera config.js a partir de API_BASE_URL e sobe o nginx em foreground.
CMD ["/bin/sh", "-c", "printf 'window.API_BASE_URL = \"%s\";\\n' \"$API_BASE_URL\" > /usr/share/nginx/html/config.js && exec nginx -g 'daemon off;'"]
