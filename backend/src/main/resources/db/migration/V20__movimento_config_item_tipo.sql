-- ===== BEGIN V20__movimento_config_item_tipo.sql =====

CREATE TABLE IF NOT EXISTS movimento_config_item_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  movimento_item_tipo_id BIGINT NOT NULL,
  cobrar BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_mov_config_item_tipo_config_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_mov_config_item_tipo_tipo_scope
    FOREIGN KEY (movimento_item_tipo_id, tenant_id)
    REFERENCES movimento_item_tipo (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_mov_config_item_tipo_scope
  ON movimento_config_item_tipo (tenant_id, movimento_config_id, movimento_item_tipo_id);

CREATE INDEX IF NOT EXISTS idx_mov_config_item_tipo_config
  ON movimento_config_item_tipo (tenant_id, movimento_config_id);

-- ===== END V20__movimento_config_item_tipo.sql =====
