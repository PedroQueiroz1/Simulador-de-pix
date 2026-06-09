-- Lote 6: cria a tabela do Transactional Outbox.
-- O schema e versionado/aplicado pelo Flyway; o Hibernate apenas valida
-- (ddl-auto=validate). Nao editar migrations antigas: cada lote adiciona uma
-- nova versao.

-- Eventos de dominio/aplicacao gravados na MESMA transacao da mudanca de
-- estado (Payment/Ledger). Um publisher assincrono le os PENDING e publica no
-- Kafka, marcando como PUBLISHED. Assim a mudanca de estado e o registro do
-- evento sao atomicos no SQL Server (ADR-025), sem depender do Kafka na
-- transacao local.
CREATE TABLE outbox_events (
    id UNIQUEIDENTIFIER NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UNIQUEIDENTIFIER NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_version INT NOT NULL,
    topic VARCHAR(120) NOT NULL,
    partition_key VARCHAR(120) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INT NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL,
    published_at DATETIME2 NULL,
    available_at DATETIME2 NOT NULL,

    CONSTRAINT PK_outbox_events PRIMARY KEY (id)
);
GO

-- Indice de varredura do publisher: busca eventos por status + janela de
-- disponibilidade (available_at), ordenando pela ordem de criacao.
CREATE INDEX IX_outbox_events_status_available_at
ON outbox_events (status, available_at, created_at);
GO

-- Idempotencia do outbox: um unico evento por (aggregate_id, event_type),
-- evitando duplicar PAYMENT_CREATED/PAYMENT_APPROVED/PAYMENT_REJECTED para o
-- mesmo pagamento (espelha a barreira do Ledger, ADR-024). Reavaliar quando
-- existir mais de um evento do mesmo tipo por aggregate.
CREATE UNIQUE INDEX UK_outbox_events_aggregate_event_type
ON outbox_events (aggregate_id, event_type);
GO
