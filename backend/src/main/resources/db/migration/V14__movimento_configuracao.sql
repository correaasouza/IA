-- ===== BEGIN V14__movimento_configuracao.sql =====

CREATE TABLE IF NOT EXISTS movimento_config (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_tipo VARCHAR(80) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  prioridade INTEGER NOT NULL DEFAULT 100,
  contexto_key VARCHAR(120),
  tipo_entidade_padrao_id BIGINT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_movimento_config_prioridade_non_negative CHECK (prioridade >= 0),
  CONSTRAINT fk_movimento_config_tipo_padrao_tenant
    FOREIGN KEY (tipo_entidade_padrao_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_id_tenant
  ON movimento_config (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tenant_tipo_ativo
  ON movimento_config (tenant_id, movimento_tipo, ativo);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tenant_contexto_prioridade
  ON movimento_config (tenant_id, movimento_tipo, contexto_key, prioridade);

CREATE TABLE IF NOT EXISTS movimento_config_empresa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_movimento_config_empresa_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_movimento_config_empresa_empresa_tenant
    FOREIGN KEY (empresa_id, tenant_id)
    REFERENCES empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_empresa_scope
  ON movimento_config_empresa (tenant_id, movimento_config_id, empresa_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_empresa_lookup
  ON movimento_config_empresa (tenant_id, empresa_id, movimento_config_id);

CREATE TABLE IF NOT EXISTS movimento_config_tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_movimento_config_tipo_entidade_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_movimento_config_tipo_entidade_tenant
    FOREIGN KEY (tipo_entidade_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_tipo_entidade_scope
  ON movimento_config_tipo_entidade (tenant_id, movimento_config_id, tipo_entidade_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tipo_entidade_lookup
  ON movimento_config_tipo_entidade (tenant_id, tipo_entidade_id, movimento_config_id);

-- ===== END V14__movimento_configuracao.sql =====
