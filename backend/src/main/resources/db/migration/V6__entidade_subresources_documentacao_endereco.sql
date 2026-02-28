-- Entidade: subrecursos iniciais (documentacao e enderecos)

CREATE UNIQUE INDEX IF NOT EXISTS ux_registro_entidade_id_tenant_empresa
  ON registro_entidade (id, tenant_id, empresa_id);

CREATE TABLE IF NOT EXISTS entidade_documentacao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  tipo_registro_federal VARCHAR(20) NOT NULL,
  registro_federal VARCHAR(40) NOT NULL,
  registro_federal_normalizado VARCHAR(40) NOT NULL,
  registro_federal_hash VARCHAR(64) NOT NULL,
  registro_federal_data_emissao DATE,
  rg VARCHAR(30),
  rg_tipo VARCHAR(30),
  rg_data_emissao DATE,
  rg_uf_emissao CHAR(2),
  registro_estadual VARCHAR(40),
  registro_estadual_data_emissao DATE,
  registro_estadual_uf CHAR(2),
  registro_estadual_contribuinte BOOLEAN NOT NULL DEFAULT FALSE,
  registro_estadual_consumidor_final BOOLEAN NOT NULL DEFAULT FALSE,
  registro_municipal VARCHAR(40),
  registro_municipal_data_emissao DATE,
  cnh VARCHAR(20),
  cnh_categoria VARCHAR(5),
  cnh_observacao VARCHAR(255),
  cnh_data_emissao DATE,
  suframa VARCHAR(30),
  rntc VARCHAR(30),
  pis VARCHAR(20),
  titulo_eleitor VARCHAR(20),
  titulo_eleitor_zona VARCHAR(10),
  titulo_eleitor_secao VARCHAR(10),
  ctps VARCHAR(20),
  ctps_serie VARCHAR(20),
  ctps_data_emissao DATE,
  ctps_uf_emissao CHAR(2),
  militar_numero VARCHAR(40),
  militar_serie VARCHAR(40),
  militar_categoria VARCHAR(20),
  numero_nif VARCHAR(40),
  motivo_nao_nif SMALLINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_documentacao_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_ent_documentacao_tipo_reg_federal
    CHECK (tipo_registro_federal IN ('CPF', 'CNPJ', 'ID_ESTRANGEIRO')),
  CONSTRAINT ck_ent_documentacao_motivo_nif
    CHECK (motivo_nao_nif IS NULL OR motivo_nao_nif IN (0, 1, 2))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_documentacao_scope_registro
  ON entidade_documentacao (tenant_id, empresa_id, registro_entidade_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_documentacao_scope_reg_federal_norm
  ON entidade_documentacao (tenant_id, empresa_id, tipo_registro_federal, registro_federal_normalizado);

CREATE INDEX IF NOT EXISTS idx_ent_documentacao_scope_hash
  ON entidade_documentacao (tenant_id, empresa_id, registro_federal_hash);

CREATE TABLE IF NOT EXISTS entidade_endereco (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  nome VARCHAR(120),
  cep VARCHAR(8),
  cep_estrangeiro VARCHAR(20),
  pais VARCHAR(80),
  pais_codigo_ibge BIGINT,
  uf CHAR(2),
  uf_codigo_ibge VARCHAR(10),
  municipio VARCHAR(120),
  municipio_codigo_ibge VARCHAR(10),
  logradouro VARCHAR(200),
  logradouro_tipo VARCHAR(40),
  numero VARCHAR(20),
  complemento VARCHAR(120),
  endereco_tipo VARCHAR(20) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  longitude NUMERIC(10,7),
  latitude NUMERIC(10,7),
  estado_provincia_regiao_estrangeiro VARCHAR(60),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_endereco_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_ent_endereco_tipo
    CHECK (endereco_tipo IN ('RESIDENCIAL', 'COMERCIAL', 'ENTREGA', 'COBRANCA', 'OUTRO', 'CORRESPONDENCIA')),
  CONSTRAINT ck_ent_endereco_latitude
    CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
  CONSTRAINT ck_ent_endereco_longitude
    CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

CREATE INDEX IF NOT EXISTS idx_ent_endereco_scope_registro
  ON entidade_endereco (tenant_id, empresa_id, registro_entidade_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_endereco_principal_por_tipo
  ON entidade_endereco (tenant_id, empresa_id, registro_entidade_id, endereco_tipo)
  WHERE principal = TRUE;
