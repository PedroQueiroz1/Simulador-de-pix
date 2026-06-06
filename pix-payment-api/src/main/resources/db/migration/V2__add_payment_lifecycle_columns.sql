-- Lote 4: adiciona as colunas de ciclo de vida do pagamento.
-- O schema continua versionado/aplicado pelo Flyway; o Hibernate apenas valida.
-- Nao editar migrations antigas: cada lote adiciona uma nova versao.

-- 1) Adiciona as colunas como NULL para nao quebrar linhas ja existentes.
ALTER TABLE pix_payments
ADD
    updated_at DATETIME2 NULL,
    processed_at DATETIME2 NULL,
    rejection_reason VARCHAR(255) NULL;
GO

-- 2) Backfill: pagamentos antigos passam a ter updated_at = created_at.
UPDATE pix_payments
SET updated_at = created_at
WHERE updated_at IS NULL;
GO

-- 3) Agora que toda linha tem valor, torna updated_at obrigatorio (bate com a entity).
ALTER TABLE pix_payments
ALTER COLUMN updated_at DATETIME2 NOT NULL;
GO
