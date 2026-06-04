# Pix Transaction Simulator

> Plataforma backend **event-driven** para simulação de pagamentos Pix, criada como projeto de portfólio com foco em Java, Spring Boot, microserviços, mensageria, idempotência, persistência, testes automatizados, CI/CD e práticas DevSecOps.

<p>
  <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/Java-17-blue?style=for-the-badge">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge">
  <img src="https://img.shields.io/badge/Architecture-Hexagonal-purple?style=for-the-badge">
</p>

---

## 🎯 Objetivo do projeto

O **Pix Transaction Simulator** tem como objetivo simular uma plataforma backend de processamento de pagamentos Pix, com foco em boas práticas utilizadas em sistemas transacionais.

A proposta é construir um projeto simples o suficiente para ser estudado e explicado em detalhes, mas completo o bastante para demonstrar conhecimentos importantes para uma vaga Backend Java Pleno, como:

- Java moderno;
- Spring Boot 3;
- arquitetura hexagonal;
- microserviços;
- mensageria com Kafka;
- idempotência em fluxos de pagamento;
- Redis;
- SQL Server;
- MongoDB;
- testes automatizados;
- CI/CD;
- DevSecOps;
- Docker;
- Kubernetes;
- Argo CD.

---

## 🧠 Contexto

Em sistemas de pagamento, uma mesma requisição pode ser enviada mais de uma vez por falhas de rede, timeout, retry do cliente ou instabilidade temporária entre serviços.

Por isso, este projeto tratará **idempotência** como uma decisão central desde o início.

A ideia é garantir que uma mesma intenção de pagamento não gere múltiplas transações indevidas.

Exemplo de problema que o projeto pretende simular:

1. O cliente solicita um pagamento Pix.
2. O backend processa a requisição.
3. A resposta falha por timeout ou queda de rede.
4. O cliente tenta novamente.
5. O sistema precisa reconhecer que aquela tentativa já existia e não deve criar um pagamento duplicado.

---

## 🧱 Arquitetura planejada

O projeto será desenvolvido inicialmente como um monorepo, contendo os serviços e artefatos de infraestrutura:

```text
pix-transaction-simulator/
├── pix-payment-api/
│   └── Serviço responsável por criar, validar e consultar pagamentos Pix
│
├── pix-notification-worker/
│   └── Serviço responsável por consumir eventos e registrar auditoria/notificações
│
├── frontend-demo/
│   └── Interface web extremamente simples apenas para demonstração visual
│
├── docs/
│   ├── specs/
│   ├── decisions/
│   └── learning/
│
├── k8s/
├── argocd/
├── docker-compose.yml
└── README.md
```

---

## 🧩 Microserviços planejados

### `pix-payment-api`

Serviço principal da plataforma.

Responsabilidades:

- receber solicitações de pagamento Pix;
- validar dados da requisição;
- aplicar regras de domínio;
- controlar idempotência;
- persistir transações no SQL Server;
- publicar eventos para o Kafka;
- expor endpoints REST para criação e consulta de pagamentos.

### `pix-notification-worker`

Serviço consumidor de eventos.

Responsabilidades:

- consumir eventos publicados pelo `pix-payment-api`;
- simular envio de notificação;
- registrar histórico e auditoria no MongoDB;
- tratar consumo duplicado de eventos;
- manter logs estruturados para rastreabilidade.

---

## 🔐 Decisões importantes

### Idempotência como requisito central

Pagamentos são fluxos críticos. Por isso, o projeto será modelado para evitar criação duplicada de transações.

A chave de idempotência será enviada via header HTTP:

```http
Idempotency-Key: 7f9d0f7a-4b2a-4d2f-9e3b-8375b4fdc321
```

Essa chave representa a intenção da requisição do cliente.

### UUIDv7 para identificadores

O projeto utilizará **UUIDv7** para identificadores como:

- `paymentId`;
- `eventId`;
- `outboxEventId`;
- `notificationAuditId`.

O UUIDv7 é adequado para este projeto porque mantém unicidade global e melhora a ordenação temporal dos registros, o que ajuda em auditoria, rastreabilidade, logs e persistência.

### Arquitetura Hexagonal

A regra de negócio será isolada de detalhes de infraestrutura.

A aplicação será organizada em torno de:

- domínio;
- casos de uso;
- portas de entrada;
- portas de saída;
- adapters REST;
- adapters de persistência;
- adapters de mensageria.

Essa abordagem facilita testes, manutenção e evolução do sistema.

---

## 🛠️ Stack planejada

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| API | Spring Web |
| Validação | Jakarta Validation |
| Persistência transacional | SQL Server |
| Cache / Idempotência | Redis |
| Mensageria | Kafka |
| Auditoria / Histórico | MongoDB |
| Testes | JUnit 5, Mockito, Testcontainers |
| Cobertura | JaCoCo |
| Containerização | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| DevSecOps | OWASP Dependency Check / análise de dependências |
| Orquestração | Kubernetes |
| GitOps | Argo CD |
| Frontend demonstrativo | HTML, CSS e JavaScript puro |

---

## 🚧 Status atual

Projeto em fase inicial de planejamento e implementação por lotes usando **Spec Driven Development**.

O desenvolvimento será feito com documentação prévia de decisões, critérios de aceite e explicações técnicas antes da implementação de cada etapa.

---

## 📦 Roadmap por lotes

### Lote 1 — Fundação do `pix-payment-api`

- Estrutura inicial do projeto;
- Java 17;
- Spring Boot 3;
- Maven multi-module;
- arquitetura hexagonal;
- endpoint REST para criação de pagamento Pix;
- validações básicas;
- `Idempotency-Key` via header;
- UUIDv7 para `paymentId`;
- persistência em memória;
- testes unitários e testes de controller;
- documentação inicial.

### Lote 2 — SQL Server

- Persistência real com SQL Server;
- Spring Data JPA;
- migrations com Flyway ou Liquibase;
- constraint única para idempotência;
- testes de integração com Testcontainers.

### Lote 3 — Redis e idempotência

- Controle de idempotência com Redis;
- armazenamento de hash/fingerprint da requisição;
- prevenção de retry com payload divergente;
- TTL para chaves;
- tratamento de requisições em processamento.

### Lote 4 — Kafka e eventos

- Publicação de eventos de pagamento;
- contrato de evento;
- serialização JSON;
- preparação para Transactional Outbox Pattern.

### Lote 5 — Worker de notificação e MongoDB

- Criação do `pix-notification-worker`;
- consumo de eventos Kafka;
- persistência de auditoria no MongoDB;
- consumidor idempotente;
- logs estruturados.

### Lote 6 — Qualidade, CI/CD e DevSecOps

- GitHub Actions;
- execução automática de testes;
- cobertura com JaCoCo;
- análise de dependências;
- Dockerfile;
- Docker Compose completo.

### Lote 7 — Kubernetes e Argo CD

- Manifests Kubernetes;
- ConfigMaps;
- Secrets de exemplo;
- Services;
- Deployments;
- exemplo de Application para Argo CD.

### Lote 8 — Frontend demonstrativo

- Tela simples para criação de Pix;
- tela simples para consulta de status;
- visualização básica de resposta da API;
- HTML, CSS e JavaScript puro.

---

## 🔄 Fluxo inicial planejado

```text
Cliente HTTP
   ↓
POST /api/v1/pix/payments
   ↓
PixPaymentController
   ↓
CreatePixPaymentUseCase
   ↓
Domínio PixPayment
   ↓
PixPaymentRepository
   ↓
Persistência
   ↓
Resposta HTTP 201
```

Nos lotes futuros, o fluxo evoluirá para:

```text
Cliente HTTP
   ↓
pix-payment-api
   ↓
SQL Server + Redis
   ↓
Outbox
   ↓
Kafka
   ↓
pix-notification-worker
   ↓
MongoDB
```

---

## 📌 Exemplo planejado de requisição

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

---

## 🧪 Testes planejados

O projeto terá testes em diferentes níveis:

- testes de domínio;
- testes de casos de uso;
- testes de controller;
- testes de integração;
- testes com banco real via Testcontainers;
- testes de idempotência;
- testes de publicação e consumo de eventos.

---

## 📚 Objetivo de aprendizado

Este projeto também tem como objetivo servir como material de estudo aprofundado.

Cada lote terá documentação explicando:

- o que foi implementado;
- por que foi implementado;
- quais decisões foram tomadas;
- quais alternativas foram descartadas;
- como o fluxo de dados funciona;
- como explicar a implementação em uma entrevista técnica.

---

## 🔗 Repositório

[github.com/PedroQueiroz1/Simulador-de-pix](https://github.com/PedroQueiroz1/Simulador-de-pix.git)
