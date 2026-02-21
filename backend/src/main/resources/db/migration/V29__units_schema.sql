CREATE TABLE IF NOT EXISTS official_unit (
  id UUID PRIMARY KEY,
  codigo_oficial VARCHAR(20) NOT NULL,
  descricao VARCHAR(160) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  origem VARCHAR(60) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_official_unit_codigo
  ON official_unit (upper(codigo_oficial));

CREATE INDEX IF NOT EXISTS idx_official_unit_ativo
  ON official_unit (ativo);

CREATE TABLE IF NOT EXISTS tenant_unit (
  id UUID PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  unidade_oficial_id UUID NOT NULL,
  sigla VARCHAR(20) NOT NULL,
  nome VARCHAR(160) NOT NULL,
  fator_para_oficial NUMERIC(24,12) NOT NULL,
  system_mirror BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_tenant_unit_fator_para_oficial_non_negative
    CHECK (fator_para_oficial >= 0),
  CONSTRAINT fk_tenant_unit_official_unit
    FOREIGN KEY (unidade_oficial_id)
    REFERENCES official_unit (id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_id_tenant
  ON tenant_unit (id, tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_tenant_sigla
  ON tenant_unit (tenant_id, lower(sigla));

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_tenant_oficial_sigla
  ON tenant_unit (tenant_id, unidade_oficial_id, lower(sigla));

CREATE INDEX IF NOT EXISTS idx_tenant_unit_tenant_oficial
  ON tenant_unit (tenant_id, unidade_oficial_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_tenant_mirror
  ON tenant_unit (tenant_id, system_mirror);

CREATE TABLE IF NOT EXISTS tenant_unit_conversion (
  id UUID PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  unidade_origem_id UUID NOT NULL,
  unidade_destino_id UUID NOT NULL,
  fator NUMERIC(24,12) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_tenant_unit_conversion_fator_positive
    CHECK (fator > 0),
  CONSTRAINT ck_tenant_unit_conversion_origem_destino_diff
    CHECK (unidade_origem_id <> unidade_destino_id),
  CONSTRAINT fk_tenant_unit_conversion_origem_tenant
    FOREIGN KEY (unidade_origem_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_tenant_unit_conversion_destino_tenant
    FOREIGN KEY (unidade_destino_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_conversion_scope
  ON tenant_unit_conversion (tenant_id, unidade_origem_id, unidade_destino_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_conversion_tenant_origem
  ON tenant_unit_conversion (tenant_id, unidade_origem_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_conversion_tenant_destino
  ON tenant_unit_conversion (tenant_id, unidade_destino_id);
