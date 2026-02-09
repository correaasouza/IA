CREATE TABLE contato_tipo_por_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL REFERENCES entidade_definicao(id),
  contato_tipo_id BIGINT NOT NULL REFERENCES contato_tipo(id),
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_contato_tipo_entidade_unique
  ON contato_tipo_por_entidade (tenant_id, entidade_definicao_id, contato_tipo_id);
