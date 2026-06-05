-- Migration inicial: cria a tabela de pagamentos Pix.
-- O schema e versionado e aplicado pelo Flyway; o Hibernate apenas valida.
CREATE TABLE pix_payments (
    id UNIQUEIDENTIFIER NOT NULL,
    payer_key VARCHAR(120) NOT NULL,
    receiver_key VARCHAR(120) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    description VARCHAR(255) NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL,

    CONSTRAINT PK_pix_payments PRIMARY KEY (id),
    CONSTRAINT UK_pix_payments_idempotency_key UNIQUE (idempotency_key)
);
