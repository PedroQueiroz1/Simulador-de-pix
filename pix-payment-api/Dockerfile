# =============================================================================
# pix-payment-api — imagem multi-stage (Lote 9)
#
# Build a partir da RAIZ do monorepo:
#   docker build -f pix-payment-api/Dockerfile -t pix-payment-api:local .
#
# Estagio 1 (build): Maven + JDK 17 compila e empacota o jar executavel.
# Estagio 2 (runtime): JRE leve, usuario nao-root, apenas o jar.
#
# Seguranca:
#   - .env e .env.* NAO entram na imagem (ver .dockerignore);
#   - nenhuma credencial e hardcodada: tudo vem por variavel de ambiente;
#   - processo roda como usuario nao-root.
# =============================================================================

# ---- Estagio 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copia primeiro os POMs (raiz + os dois modulos) para aproveitar o cache de
# camadas: enquanto os POMs nao mudam, as dependencias baixadas sao reusadas.
# O parent agrega os dois modulos, por isso ambos os POMs precisam existir.
COPY pom.xml ./
COPY pix-payment-api/pom.xml pix-payment-api/pom.xml
COPY pix-notification-worker/pom.xml pix-notification-worker/pom.xml

# Pre-baixa as dependencias do modulo (e do parent via -am).
RUN mvn -B -pl pix-payment-api -am dependency:go-offline -DskipTests

# Copia o codigo-fonte do modulo e empacota (sem testes no build da imagem;
# os testes rodam no CI com Testcontainers).
COPY pix-payment-api/src pix-payment-api/src
RUN mvn -B -pl pix-payment-api -am clean package -DskipTests

# ---- Estagio 2: runtime ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Usuario/grupo nao-root.
RUN groupadd --system spring && useradd --system --gid spring spring

# Apenas o jar executavel do estagio de build.
COPY --from=build /workspace/pix-payment-api/target/*.jar app.jar

USER spring:spring

# Porta HTTP da API (informativa; a porta efetiva vem de PAYMENT_API_PORT).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
