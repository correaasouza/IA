-- Fundacao do agregado de entidades (fase 1)
-- Mantem compatibilidade com API atual e prepara expansao por blocos.

-- 1) Catalogo de tratamento
CREATE TABLE IF NOT EXISTS entidade_tratamento (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  descricao VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_entidade_tratamento_tenant_descricao
  ON entidade_tratamento (tenant_id, lower(descricao));

CREATE INDEX IF NOT EXISTS idx_entidade_tratamento_tenant_ativo
  ON entidade_tratamento (tenant_id, ativo);

CREATE UNIQUE INDEX IF NOT EXISTS ux_entidade_tratamento_id_tenant
  ON entidade_tratamento (id, tenant_id);

-- 2) Evolucao da raiz registro_entidade
ALTER TABLE registro_entidade
  ADD COLUMN IF NOT EXISTS empresa_id BIGINT,
  ADD COLUMN IF NOT EXISTS alerta VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS observacao TEXT,
  ADD COLUMN IF NOT EXISTS parecer TEXT,
  ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(60),
  ADD COLUMN IF NOT EXISTS texto_termo_quitacao VARCHAR(4096),
  ADD COLUMN IF NOT EXISTS tratamento_id BIGINT,
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Backfill de empresa_id:
-- 1) primeira empresa vinculada ao agrupador da configuracao
-- 2) fallback para matriz do tenant
-- 3) fallback para primeira empresa do tenant
UPDATE registro_entidade re
SET empresa_id = COALESCE(
  (
    SELECT aei.empresa_id
    FROM tipo_entidade_config_agrupador teca
    JOIN agrupador_empresa_item aei
      ON aei.tenant_id = teca.tenant_id
     AND aei.config_type = 'TIPO_ENTIDADE'
     AND aei.config_id = teca.tipo_entidade_id
     AND aei.agrupador_id = teca.agrupador_id
    WHERE teca.id = re.tipo_entidade_config_agrupador_id
      AND teca.tenant_id = re.tenant_id
    ORDER BY aei.id
    LIMIT 1
  ),
  (
    SELECT e.id
    FROM empresa e
    WHERE e.tenant_id = re.tenant_id
    ORDER BY CASE WHEN e.tipo = 'MATRIZ' THEN 0 ELSE 1 END, e.id
    LIMIT 1
  )
)
WHERE re.empresa_id IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_registro_entidade_empresa_tenant'
      AND table_name = 'registro_entidade'
  ) THEN
    ALTER TABLE registro_entidade
      ADD CONSTRAINT fk_registro_entidade_empresa_tenant
      FOREIGN KEY (empresa_id, tenant_id)
      REFERENCES empresa (id, tenant_id)
      ON DELETE RESTRICT;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_registro_entidade_tratamento_tenant'
      AND table_name = 'registro_entidade'
  ) THEN
    ALTER TABLE registro_entidade
      ADD CONSTRAINT fk_registro_entidade_tratamento_tenant
      FOREIGN KEY (tratamento_id, tenant_id)
      REFERENCES entidade_tratamento (id, tenant_id)
      ON DELETE SET NULL;
  END IF;
END $$;

ALTER TABLE registro_entidade
  ALTER COLUMN empresa_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_registro_entidade_scope_empresa
  ON registro_entidade (tenant_id, empresa_id, tipo_entidade_config_agrupador_id, ativo, codigo);

CREATE UNIQUE INDEX IF NOT EXISTS ux_registro_entidade_empresa_codigo_barras
  ON registro_entidade (tenant_id, empresa_id, codigo_barras)
  WHERE codigo_barras IS NOT NULL;

-- 3) Configuracao por grupo/campo da ficha de entidade por configuracao de agrupador
CREATE TABLE IF NOT EXISTS entidade_form_group_config (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  group_key VARCHAR(80) NOT NULL,
  label VARCHAR(120),
  ordem INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  collapsed_by_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_form_group_cfg_scope
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_form_group_cfg_scope_key
  ON entidade_form_group_config (tenant_id, tipo_entidade_config_agrupador_id, group_key);

-- Necessario para FK composta (group_config_id, tenant_id) em entidade_form_field_config
CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_form_group_cfg_id_tenant
  ON entidade_form_group_config (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_ent_form_group_cfg_scope_ordem
  ON entidade_form_group_config (tenant_id, tipo_entidade_config_agrupador_id, ordem);

CREATE TABLE IF NOT EXISTS entidade_form_field_config (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  group_config_id BIGINT NOT NULL,
  field_key VARCHAR(120) NOT NULL,
  label VARCHAR(160),
  ordem INTEGER NOT NULL DEFAULT 0,
  visible BOOLEAN NOT NULL DEFAULT TRUE,
  editable BOOLEAN NOT NULL DEFAULT TRUE,
  required BOOLEAN NOT NULL DEFAULT FALSE,
  default_value_json TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_form_field_cfg_group
    FOREIGN KEY (group_config_id, tenant_id)
    REFERENCES entidade_form_group_config (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_form_field_cfg_group_field
  ON entidade_form_field_config (group_config_id, field_key);

CREATE INDEX IF NOT EXISTS idx_ent_form_field_cfg_group_ordem
  ON entidade_form_field_config (tenant_id, group_config_id, ordem);
