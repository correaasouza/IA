-- ===== BEGIN V18__movimento_estoque_drop_data_movimento.sql =====

DROP INDEX IF EXISTS idx_movimento_estoque_tenant_empresa_data;

ALTER TABLE movimento_estoque
  DROP COLUMN IF EXISTS data_movimento;

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_id
  ON movimento_estoque (tenant_id, empresa_id, id DESC);

-- ===== END V18__movimento_estoque_drop_data_movimento.sql =====
