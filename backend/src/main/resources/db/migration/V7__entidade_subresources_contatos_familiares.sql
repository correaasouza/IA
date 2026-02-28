-- Entidade: subrecursos de contatos/formas e familiares

CREATE TABLE IF NOT EXISTS entidade_contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  nome VARCHAR(120),
  cargo VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_contato_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_contato_id_scope
  ON entidade_contato (id, tenant_id, empresa_id, registro_entidade_id);

CREATE INDEX IF NOT EXISTS idx_ent_contato_scope_registro
  ON entidade_contato (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_contato_forma (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  contato_id BIGINT NOT NULL,
  tipo_contato VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  valor_normalizado VARCHAR(200) NOT NULL,
  preferencial BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_contato_forma_contato_scope
    FOREIGN KEY (contato_id, tenant_id, empresa_id, registro_entidade_id)
    REFERENCES entidade_contato (id, tenant_id, empresa_id, registro_entidade_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_ent_contato_forma_tipo
    CHECK (tipo_contato IN ('EMAIL', 'FONE_CELULAR', 'FONE_RESIDENCIAL', 'FONE_COMERCIAL', 'FACEBOOK', 'WHATSAPP'))
);

CREATE INDEX IF NOT EXISTS idx_ent_contato_forma_scope_contato
  ON entidade_contato_forma (tenant_id, empresa_id, contato_id);

CREATE INDEX IF NOT EXISTS idx_ent_contato_forma_scope_tipo_valor
  ON entidade_contato_forma (tenant_id, empresa_id, tipo_contato, valor_normalizado);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_contato_forma_preferencial_tipo
  ON entidade_contato_forma (tenant_id, empresa_id, contato_id, tipo_contato)
  WHERE preferencial = TRUE;

CREATE TABLE IF NOT EXISTS entidade_familiar (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  entidade_parente_id BIGINT NOT NULL,
  dependente BOOLEAN NOT NULL DEFAULT FALSE,
  parentesco VARCHAR(20) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_familiar_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ent_familiar_parente_scope
    FOREIGN KEY (entidade_parente_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE RESTRICT,
  CONSTRAINT ck_ent_familiar_parentesco
    CHECK (parentesco IN (
      'PAI','FILHO','IRMAO','IRMA','TIO','TIA','PRIMO','PRIMA',
      'VO','VOMAE','BISAVO','BISAVOMAE','OUTROS'
    ))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_familiar_scope_unique
  ON entidade_familiar (tenant_id, empresa_id, registro_entidade_id, entidade_parente_id, parentesco);

CREATE INDEX IF NOT EXISTS idx_ent_familiar_scope_registro
  ON entidade_familiar (tenant_id, empresa_id, registro_entidade_id);
