-- ===== BEGIN V7__catalog_item_code_seq.sql =====

CREATE TABLE IF NOT EXISTS catalog_item_code_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_item_code_seq_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_item_code_seq_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_item_code_seq_scope
  ON catalog_item_code_seq (tenant_id, catalog_configuration_id, agrupador_empresa_id);

-- ===== END V7__catalog_item_code_seq.sql =====
