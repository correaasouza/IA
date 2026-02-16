-- ===== BEGIN V12__catalog_stock_adjustment_type_scope.sql =====

ALTER TABLE catalog_stock_adjustment
  ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'ENTRADA',
  ADD COLUMN IF NOT EXISTS estoque_origem_agrupador_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_origem_tipo_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_origem_filial_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_agrupador_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_tipo_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_filial_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'ck_catalog_stock_adjustment_tipo'
  ) THEN
    ALTER TABLE catalog_stock_adjustment
      ADD CONSTRAINT ck_catalog_stock_adjustment_tipo
      CHECK (tipo IN ('ENTRADA', 'SAIDA', 'TRANSFERENCIA'));
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_catalog_stock_adjustment_type
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, tipo, active, ordem);

-- ===== END V12__catalog_stock_adjustment_type_scope.sql =====

