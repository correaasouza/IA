CREATE TABLE tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_tipo_entidade_tenant ON tipo_entidade (tenant_id);

CREATE TABLE campo_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  nome VARCHAR(120) NOT NULL,
  label VARCHAR(120),
  tipo VARCHAR(40) NOT NULL,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  tamanho INTEGER,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_campo_definicao_tenant ON campo_definicao (tenant_id);
CREATE INDEX idx_campo_definicao_tipo ON campo_definicao (tipo_entidade_id);

CREATE TABLE registro_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_registro_entidade_tenant ON registro_entidade (tenant_id);
CREATE INDEX idx_registro_entidade_tipo ON registro_entidade (tipo_entidade_id);

CREATE TABLE registro_campo_valor (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL REFERENCES registro_entidade(id),
  campo_definicao_id BIGINT NOT NULL REFERENCES campo_definicao(id),
  valor_texto TEXT,
  valor_numero NUMERIC(18,2),
  valor_data DATE,
  valor_booleano BOOLEAN,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_registro_campo_tenant ON registro_campo_valor (tenant_id);

CREATE TABLE config_coluna (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  screen_id VARCHAR(120) NOT NULL,
  scope_tipo VARCHAR(20) NOT NULL,
  scope_valor VARCHAR(120),
  config_json TEXT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_config_coluna_tenant ON config_coluna (tenant_id);

CREATE TABLE config_formulario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  screen_id VARCHAR(120) NOT NULL,
  scope_tipo VARCHAR(20) NOT NULL,
  scope_valor VARCHAR(120),
  config_json TEXT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_config_formulario_tenant ON config_formulario (tenant_id);
