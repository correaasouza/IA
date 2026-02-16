-- ===== BEGIN V9__catalog_movement.sql =====

CREATE TABLE IF NOT EXISTS catalog_movement (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalogo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  origem_movimentacao_tipo VARCHAR(40) NOT NULL,
  origem_movimentacao_codigo VARCHAR(120),
  origem_movimento_item_codigo VARCHAR(120),
  data_hora_movimentacao TIMESTAMP NOT NULL DEFAULT NOW(),
  observacao VARCHAR(255),
  idempotency_key VARCHAR(180) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_movement_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_movement_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_movement_idempotency
  ON catalog_movement (tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_catalog_data
  ON catalog_movement (tenant_id, catalog_type, catalogo_id, data_hora_movimentacao DESC);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_origem
  ON catalog_movement (tenant_id, origem_movimentacao_tipo, origem_movimentacao_codigo);

CREATE TABLE IF NOT EXISTS catalog_movement_line (
  id BIGSERIAL PRIMARY KEY,
  movement_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  metric_type VARCHAR(20) NOT NULL,
  estoque_tipo_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  before_value NUMERIC(19,6) NOT NULL,
  delta NUMERIC(19,6) NOT NULL,
  after_value NUMERIC(19,6) NOT NULL,
  CONSTRAINT fk_catalog_movement_line_movement
    FOREIGN KEY (movement_id)
    REFERENCES catalog_movement (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_movement_line_stock_type
    FOREIGN KEY (estoque_tipo_id)
    REFERENCES catalog_stock_type (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_movement_line_filial
    FOREIGN KEY (filial_id)
    REFERENCES empresa (id)
    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_line_movement
  ON catalog_movement_line (movement_id);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_line_scope
  ON catalog_movement_line (tenant_id, agrupador_empresa_id, estoque_tipo_id, filial_id);

-- ===== END V9__catalog_movement.sql =====
