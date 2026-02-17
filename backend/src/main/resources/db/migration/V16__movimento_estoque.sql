-- ===== BEGIN V16__movimento_estoque.sql =====

CREATE TABLE IF NOT EXISTS movimento_estoque (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  tipo_movimento VARCHAR(80) NOT NULL,
  data_movimento DATE,
  status VARCHAR(80),
  nome VARCHAR(120) NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  tipo_entidade_padrao_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_movimento_estoque_tipo
    CHECK (tipo_movimento = 'MOVIMENTO_ESTOQUE'),
  CONSTRAINT fk_movimento_estoque_empresa_tenant
    FOREIGN KEY (empresa_id, tenant_id)
    REFERENCES empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimento_estoque_config_tenant
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimento_estoque_tipo_entidade_tenant
    FOREIGN KEY (tipo_entidade_padrao_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_id_tenant
  ON movimento_estoque (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_data
  ON movimento_estoque (tenant_id, empresa_id, data_movimento DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_nome
  ON movimento_estoque (tenant_id, nome);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_nome
  ON movimento_estoque (tenant_id, empresa_id, nome);

-- ===== END V16__movimento_estoque.sql =====
