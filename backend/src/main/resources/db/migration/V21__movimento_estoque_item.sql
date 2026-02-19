-- ===== BEGIN V21__movimento_estoque_item.sql =====

CREATE TABLE IF NOT EXISTS movimento_estoque_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_estoque_id BIGINT NOT NULL,
  movimento_item_tipo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  catalog_codigo_snapshot BIGINT NOT NULL,
  catalog_nome_snapshot VARCHAR(200) NOT NULL,
  quantidade NUMERIC(19,6) NOT NULL DEFAULT 0,
  valor_unitario NUMERIC(19,6) NOT NULL DEFAULT 0,
  valor_total NUMERIC(19,6) NOT NULL DEFAULT 0,
  cobrar BOOLEAN NOT NULL DEFAULT TRUE,
  ordem INTEGER NOT NULL DEFAULT 0,
  observacao VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_mov_estoque_item_catalog_type
    CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_mov_estoque_item_quantidade_non_negative
    CHECK (quantidade >= 0),
  CONSTRAINT ck_mov_estoque_item_preco_non_negative
    CHECK (valor_unitario >= 0 AND valor_total >= 0),
  CONSTRAINT ck_mov_estoque_item_ordem_non_negative
    CHECK (ordem >= 0),
  CONSTRAINT ck_mov_estoque_item_cobrar_zero
    CHECK (cobrar = TRUE OR (valor_unitario = 0 AND valor_total = 0)),
  CONSTRAINT fk_mov_estoque_item_movimento_scope
    FOREIGN KEY (movimento_estoque_id, tenant_id)
    REFERENCES movimento_estoque (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_mov_estoque_item_tipo_scope
    FOREIGN KEY (movimento_item_tipo_id, tenant_id)
    REFERENCES movimento_item_tipo (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_movimento
  ON movimento_estoque_item (tenant_id, movimento_estoque_id, ordem, id);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_tipo
  ON movimento_estoque_item (tenant_id, movimento_item_tipo_id);

-- ===== END V21__movimento_estoque_item.sql =====
