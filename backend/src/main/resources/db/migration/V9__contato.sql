CREATE TABLE contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_registro_id BIGINT NOT NULL REFERENCES entidade_registro(id),
  tipo VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_contato_tenant ON contato (tenant_id);
CREATE INDEX idx_contato_entidade ON contato (entidade_registro_id);
