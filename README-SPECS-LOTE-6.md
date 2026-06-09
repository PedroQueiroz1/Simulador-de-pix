# Specs — Lote 6 — Kafka + Transactional Outbox

## Contexto atual

O projeto já possui:

- Lote 1: fundação do `pix-payment-api`, Arquitetura Hexagonal, criação de pagamento, UUIDv7 e validações.
- Lote 2: SQL Server, JPA, Flyway, Testcontainers, Entity separada do domínio e constraint única de idempotência.
- Lote 3: Redis e idempotência completa para criação de pagamento.
- Lote 4: ciclo de vida do pagamento, consulta por ID e processamento simulado.
- Lote 5: Ledger transacional, append-only, débito/crédito, fechamento em zero e idempotência por `paymentId + operationType`.

## Objetivo do Lote 6

Adicionar publicação confiável de eventos usando **Transactional Outbox Pattern** e **Kafka**.

A aplicação não deve publicar eventos diretamente no Kafka dentro do fluxo principal de negócio.

Em vez disso, deve:

```text
1. Executar regra de negócio.
2. Salvar Payment/Ledger no SQL Server.
3. Salvar evento na tabela outbox_events na mesma transação.
4. Um publisher assíncrono lê a outbox.
5. O publisher publica no Kafka.
6. Ao publicar com sucesso, marca o evento como PUBLISHED.
```

## O que será implementado

- Tabela `outbox_events`.
- Domínio/modelo de Outbox.
- Porta `OutboxRepository`.
- Adapter JPA para Outbox.
- Criação de eventos de domínio/aplicação na mesma transação do caso de uso.
- Kafka producer.
- Publisher assíncrono/polling da Outbox.
- Status de publicação: `PENDING`, `PUBLISHED`, `FAILED`.
- Controle de tentativas e erro de publicação.
- Docker Compose com Kafka.
- Configuração via variáveis de ambiente.
- Testes unitários e de integração.

## O que NÃO será implementado neste lote

- `pix-notification-worker`.
- MongoDB.
- Kafka consumer.
- Dead Letter Topic completo.
- Schema Registry.
- Avro/Protobuf.
- Autenticação/autorização.
- CI/CD.
- Kubernetes.
- Argo CD.
- Frontend.
- Observabilidade avançada.
- Cleanup de classes obsoletas.

## Regras de segurança

- Não versionar credenciais reais.
- Não colocar login, senha, nome real de banco, tokens ou secrets hardcoded.
- `.env` deve continuar ignorado.
- `.env.example`, se existir, deve usar somente valores fictícios.
- `docs/` deve continuar conforme decisão atual do usuário: se estiver ignorado no Git, manter ignorado.

## Regra sobre README principal

Não atualizar obrigatoriamente o README.md principal.

Documentação incremental local em:

```text
docs/specs/
docs/decisions/
docs/learning/lote-6-implementation-notes.md
```

## Validação final

```bash
mvn clean test
```
