CREATE TABLE entidade_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_entidade_definicao_unique ON entidade_definicao (tenant_id, codigo);
CREATE INDEX idx_entidade_definicao_tenant ON entidade_definicao (tenant_id);

CREATE TABLE entidade_registro (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL REFERENCES entidade_definicao(id),
  nome VARCHAR(200) NOT NULL,
  apelido VARCHAR(200),
  cpf_cnpj VARCHAR(20) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_entidade_registro_tenant ON entidade_registro (tenant_id);
CREATE INDEX idx_entidade_registro_definicao ON entidade_registro (entidade_definicao_id);
