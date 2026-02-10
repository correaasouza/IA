ALTER TABLE tipo_entidade ADD COLUMN IF NOT EXISTS codigo VARCHAR(40);
ALTER TABLE tipo_entidade ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE tipo_entidade
SET codigo = UPPER(REGEXP_REPLACE(nome, '\s+', '_', 'g'))
WHERE codigo IS NULL;

WITH duplicates AS (
  SELECT tenant_id, codigo, MIN(id) AS keep_id
  FROM tipo_entidade
  GROUP BY tenant_id, codigo
  HAVING COUNT(*) > 1
)
DELETE FROM tipo_entidade t
USING duplicates d
WHERE t.tenant_id = d.tenant_id
  AND t.codigo = d.codigo
  AND t.id <> d.keep_id;

ALTER TABLE tipo_entidade ALTER COLUMN codigo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tipo_entidade_unique_codigo
  ON tipo_entidade (tenant_id, codigo);

CREATE TABLE pessoa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  apelido VARCHAR(200),
  cpf VARCHAR(11),
  cnpj VARCHAR(14),
  id_estrangeiro VARCHAR(40),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_pessoa_tenant ON pessoa (tenant_id);
CREATE UNIQUE INDEX idx_pessoa_cpf_unique ON pessoa (tenant_id, cpf) WHERE cpf IS NOT NULL;
CREATE UNIQUE INDEX idx_pessoa_cnpj_unique ON pessoa (tenant_id, cnpj) WHERE cnpj IS NOT NULL;
CREATE UNIQUE INDEX idx_pessoa_estrangeiro_unique ON pessoa (tenant_id, id_estrangeiro) WHERE id_estrangeiro IS NOT NULL;

CREATE TABLE entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_entidade_tenant ON entidade (tenant_id);
CREATE INDEX idx_entidade_tipo ON entidade (tipo_entidade_id);
CREATE INDEX idx_entidade_pessoa ON entidade (pessoa_id);

CREATE TABLE pessoa_contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
  tipo VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_pessoa_contato_tenant ON pessoa_contato (tenant_id);
CREATE INDEX idx_pessoa_contato_pessoa ON pessoa_contato (pessoa_id);

CREATE TABLE tipo_entidade_campo_regra (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  campo VARCHAR(60) NOT NULL,
  habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  requerido BOOLEAN NOT NULL DEFAULT FALSE,
  visivel BOOLEAN NOT NULL DEFAULT TRUE,
  editavel BOOLEAN NOT NULL DEFAULT TRUE,
  label VARCHAR(120),
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_tipo_entidade_campo_tenant ON tipo_entidade_campo_regra (tenant_id);
CREATE INDEX idx_tipo_entidade_campo_tipo ON tipo_entidade_campo_regra (tipo_entidade_id);
