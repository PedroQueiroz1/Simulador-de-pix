# Pix Transaction Simulator

> Plataforma backend **event-driven** para simulação de pagamentos Pix, criada como projeto de portfólio com foco em Java, Spring Boot, microserviços, mensageria, idempotência, persistência, testes automatizados, CI/CD e práticas DevSecOps.

<p>
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-purple?style=for-the-badge">
</p>

---

## 🎯 Objetivo do projeto

O **Pix Transaction Simulator** simula uma plataforma backend de processamento de pagamentos Pix, com foco nas boas práticas de sistemas transacionais.

A proposta é construir um projeto simples o suficiente para ser estudado e explicado em detalhes, mas completo o bastante para demonstrar conhecimentos de uma vaga **Backend Java Pleno**: Java moderno, Spring Boot 3, arquitetura hexagonal, microserviços, idempotência, testes automatizados, CI/CD e DevSecOps.

> ⚠️ Este projeto **simula** um fluxo de pagamento. Ele **não** é uma integração real com o Pix/Bacen.

---

## 📦 Sobre este lote — Lote 1

Este repositório, no estado atual, entrega o **Lote 1**: a **fundação** do microserviço `pix-payment-api`.

O que o Lote 1 faz:

1. Recebe uma requisição HTTP para criação de pagamento Pix.
2. Valida os dados básicos da requisição (Jakarta Validation).
3. Exige o header `Idempotency-Key`.
4. Gera o `paymentId` como **UUIDv7**.
5. Cria o pagamento com status `CREATED`.
6. Persiste o pagamento **em memória**.
7. Retorna **HTTP 201** com os dados do pagamento criado.
8. Possui testes automatizados de domínio, caso de uso, adapter de ID e controller.

O que o Lote 1 **ainda não** faz (próximos lotes): SQL Server, Spring Data JPA, MongoDB, Redis, Kafka, Docker, Kubernetes, Argo CD, GitHub Actions, Swagger/OpenAPI, autenticação, autorização e frontend.

---

## 🛠️ Stack do Lote 1

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.x |
| Build | Maven multi-module |
| API REST | `spring-boot-starter-web` |
| Validação | `spring-boot-starter-validation` (Jakarta Validation) |
| Geração de ID | `com.github.f4b6a3:uuid-creator` (UUIDv7), isolada em adapter |
| Persistência | Em memória (`ConcurrentHashMap`) — temporária |
| Testes | JUnit 5, Mockito, Spring Boot Test (MockMvc) |
| Arquitetura | Hexagonal (ports & adapters) |

---

## 🧱 Estrutura do repositório

```text
pix-transaction-simulator/
├── pom.xml                 # Maven parent multi-module
├── pix-payment-api/        # microserviço do Lote 1
│   ├── pom.xml
│   └── src/
│       ├── main/java/io/pixsimulator/payment/
│       │   ├── domain/          # regras de negócio puras (sem Spring)
│       │   ├── application/     # ports (in/out), DTOs e caso de uso
│       │   ├── adapter/         # web (entrada) e persistência/id (saída)
│       │   └── config/          # wiring do caso de uso
│       └── test/java/...        # testes de domínio, use case, id e controller
└── docs/                   # specs, decisões (ADRs) e material de estudo
```

---

## ▶️ Como rodar os testes

Na raiz do repositório:

```bash
mvn clean test
```

Resultado esperado: **25 testes**, `BUILD SUCCESS`.

---

## 🚀 Como subir a aplicação

```bash
mvn -pl pix-payment-api spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

> Também é possível empacotar e rodar o jar:
> ```bash
> mvn clean package
> java -jar pix-payment-api/target/pix-payment-api-0.0.1-SNAPSHOT.jar
> ```

---

## 📡 Endpoint

```http
POST /api/v1/pix/payments
```

Header obrigatório:

```http
Idempotency-Key: 7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321
```

### ✅ Exemplo de `curl` válido

```bash
curl -X POST http://localhost:8080/api/v1/pix/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321" \
  -d '{
    "payerKey": "11111111111",
    "receiverKey": "22222222222",
    "amount": 150.75,
    "description": "Pagamento de teste"
  }'
```

Resposta — **HTTP 201 Created**:

```json
{
  "paymentId": "01977a97-3c1f-7b48-a4e6-2b8d4e7b0b41",
  "status": "CREATED",
  "payerKey": "11111111111",
  "receiverKey": "22222222222",
  "amount": 150.75,
  "description": "Pagamento de teste"
}
```

> A `idempotencyKey` **não** aparece na resposta: ela é metadado da requisição.

### ❌ Exemplo de `curl` inválido (sem o header `Idempotency-Key`)

```bash
curl -X POST http://localhost:8080/api/v1/pix/payments \
  -H "Content-Type: application/json" \
  -d '{
    "payerKey": "11111111111",
    "receiverKey": "22222222222",
    "amount": 150.75,
    "description": "Pagamento de teste"
  }'
```

Resposta — **HTTP 400 Bad Request** (sem stack trace):

```json
{
  "message": "Erro de validacao da requisicao",
  "errors": ["Header obrigatorio ausente: Idempotency-Key"]
}
```

---

## 🔄 Fluxo de dados (resumo)

```text
Cliente HTTP
   ↓  POST /api/v1/pix/payments  (+ header Idempotency-Key)
PixPaymentController          → valida a request e extrai o header
   ↓  CreatePixPaymentCommand
CreatePixPaymentUseCase       → porta de entrada (interface)
   ↓
CreatePixPaymentService       → orquestra o caso de uso
   ↓  IdGenerator.generate()  → gera o UUIDv7 (adapter isolado)
   ↓  PixPayment.create(...)  → aplica as regras de negócio (domínio)
   ↓  PixPaymentRepository.save(...)  → persiste (em memória no Lote 1)
   ↓  CreatePixPaymentResult
PixPaymentController          → converte em CreatePixPaymentResponse
   ↓
HTTP 201 Created
```

O controller é apenas um detalhe de entrega HTTP; a regra de criação vive no domínio. O caso de uso depende de **interfaces** (`PixPaymentRepository`, `IdGenerator`), não de tecnologias concretas — por isso trocar memória por SQL Server, ou UUIDv7 por outra estratégia, não muda o caso de uso.

---

## 🔐 Idempotência neste lote (honestidade técnica)

> O Lote 1 é uma **fundação educacional e local**. Ele **prepara** o fluxo para idempotência — exige o header `Idempotency-Key`, carrega essa chave até o caso de uso e a associa ao pagamento criado — **mas ainda não garante idempotência forte**.
>
> Como o Lote 1 usa persistência em memória, **sem Redis e sem SQL Server**, ele ainda não garante que duas requisições com a mesma chave não criem dois pagamentos em condições reais. Essa garantia será implementada nos próximos lotes com **constraint única no SQL Server** e **Redis** para controle de retry.

`Idempotency-Key` e `UUIDv7` são **identificadores**, não mecanismos de segurança: não devem ser usados como token, senha, autorização ou prova de identidade.

---

## 📚 Documentação

- Especificações do Lote 1: [docs/specs/](docs/specs/)
- Decisões de arquitetura (ADRs): [docs/decisions/](docs/decisions/)
- Material de estudo: [docs/learning/](docs/learning/)
- Notas de implementação do Lote 1: [docs/learning/lote-1-implementation-notes.md](docs/learning/lote-1-implementation-notes.md)

---

## 🔗 Repositório

[github.com/PedroQueiroz1/Simulador-de-pix](https://github.com/PedroQueiroz1/Simulador-de-pix.git)
