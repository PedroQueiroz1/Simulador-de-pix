-- Lote 5: cria as tabelas do Ledger transacional.
-- O schema e versionado/aplicado pelo Flyway; o Hibernate apenas valida
-- (ddl-auto=validate). Nao editar migrations antigas: cada lote adiciona uma
-- nova versao.

-- Conjunto de lancamentos de uma operacao (ex.: liquidacao de um Pix aprovado).
-- A constraint unica (payment_id, operation_type) e a barreira final de
-- idempotencia do Ledger (ADR-024): um unico PIX_SETTLEMENT por pagamento.
CREATE TABLE ledger_transactions (
    id UNIQUEIDENTIFIER NOT NULL,
    payment_id UNIQUEIDENTIFIER NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    created_at DATETIME2 NOT NULL,

    CONSTRAINT PK_ledger_transactions PRIMARY KEY (id),
    CONSTRAINT UK_ledger_transactions_payment_operation UNIQUE (payment_id, operation_type),
    CONSTRAINT FK_ledger_transactions_payment FOREIGN KEY (payment_id) REFERENCES pix_payments(id)
);
GO

-- Linhas (DEBIT/CREDIT) de cada transacao. amount sempre positivo; o sentido
-- vem da coluna direction (ADR-022).
CREATE TABLE ledger_entries (
    id UNIQUEIDENTIFIER NOT NULL,
    ledger_transaction_id UNIQUEIDENTIFIER NOT NULL,
    payment_id UNIQUEIDENTIFIER NOT NULL,
    account_key VARCHAR(120) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_at DATETIME2 NOT NULL,

    CONSTRAINT PK_ledger_entries PRIMARY KEY (id),
    CONSTRAINT FK_ledger_entries_transaction FOREIGN KEY (ledger_transaction_id) REFERENCES ledger_transactions(id),
    CONSTRAINT FK_ledger_entries_payment FOREIGN KEY (payment_id) REFERENCES pix_payments(id)
);
GO
