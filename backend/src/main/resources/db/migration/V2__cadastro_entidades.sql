-- ===== BEGIN V2__cadastro_entidades.sql =====

CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_ent_cfg_agrupador_id_tenant
  ON tipo_entidade_config_agrupador (id, tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_id_tenant
  ON pessoa (id, tenant_id);

ALTER TABLE pessoa
  ADD COLUMN IF NOT EXISTS tipo_registro VARCHAR(20),
  ADD COLUMN IF NOT EXISTS registro_federal VARCHAR(40),
  ADD COLUMN IF NOT EXISTS registro_federal_normalizado VARCHAR(40);

UPDATE pessoa
SET
  tipo_registro = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN 'CPF'
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN 'CNPJ'
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN 'ID_ESTRANGEIRO'
    ELSE tipo_registro
  END,
  registro_federal = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN cpf
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN cnpj
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN UPPER(BTRIM(id_estrangeiro))
    ELSE registro_federal
  END,
  registro_federal_normalizado = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN cpf
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN cnpj
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN UPPER(BTRIM(id_estrangeiro))
    ELSE registro_federal_normalizado
  END
WHERE tipo_registro IS NULL
   OR registro_federal IS NULL
   OR registro_federal_normalizado IS NULL;

ALTER TABLE pessoa
  ALTER COLUMN tipo_registro SET NOT NULL,
  ALTER COLUMN registro_federal SET NOT NULL,
  ALTER COLUMN registro_federal_normalizado SET NOT NULL;

ALTER TABLE pessoa
  DROP CONSTRAINT IF EXISTS ck_pessoa_tipo_registro;

ALTER TABLE pessoa
  ADD CONSTRAINT ck_pessoa_tipo_registro
    CHECK (tipo_registro IN ('CPF', 'CNPJ', 'ID_ESTRANGEIRO'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_tenant_tipo_registro_federal_norm
  ON pessoa (tenant_id, tipo_registro, registro_federal_normalizado);

CREATE INDEX IF NOT EXISTS idx_pessoa_tenant_tipo_registro_federal_norm
  ON pessoa (tenant_id, tipo_registro, registro_federal_normalizado);

CREATE TABLE registro_entidade_codigo_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_reg_ent_seq_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_registro_entidade_codigo_seq_scope
  ON registro_entidade_codigo_seq (tenant_id, tipo_entidade_config_agrupador_id);

CREATE TABLE grupo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  parent_id BIGINT,
  nome VARCHAR(120) NOT NULL,
  nome_normalizado VARCHAR(120) NOT NULL,
  nivel INTEGER NOT NULL DEFAULT 0,
  path VARCHAR(900) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_grupo_entidade_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_grupo_entidade_id_scope
  ON grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id);

ALTER TABLE grupo_entidade
  ADD CONSTRAINT fk_grupo_entidade_parent_scope
    FOREIGN KEY (parent_id, tenant_id, tipo_entidade_config_agrupador_id)
    REFERENCES grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX ux_grupo_entidade_nome_parent_ativo
  ON grupo_entidade (
    tenant_id,
    tipo_entidade_config_agrupador_id,
    COALESCE(parent_id, 0),
    nome_normalizado
  )
  WHERE ativo = TRUE;

CREATE INDEX idx_grupo_entidade_scope_parent_ordem
  ON grupo_entidade (tenant_id, tipo_entidade_config_agrupador_id, parent_id, ordem);

CREATE INDEX idx_grupo_entidade_scope_path
  ON grupo_entidade (tenant_id, tipo_entidade_config_agrupador_id, path);

CREATE TABLE registro_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  codigo BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL,
  grupo_entidade_id BIGINT,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_registro_entidade_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_registro_entidade_pessoa_tenant
    FOREIGN KEY (pessoa_id, tenant_id)
    REFERENCES pessoa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_registro_entidade_grupo_scope
    FOREIGN KEY (grupo_entidade_id, tenant_id, tipo_entidade_config_agrupador_id)
    REFERENCES grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_registro_entidade_codigo_scope
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, codigo);

CREATE INDEX idx_registro_entidade_scope_ativo_codigo
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, ativo, codigo);

CREATE INDEX idx_registro_entidade_scope_grupo
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, grupo_entidade_id);

CREATE INDEX idx_registro_entidade_scope_pessoa
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, pessoa_id);

-- ===== END V2__cadastro_entidades.sql =====
