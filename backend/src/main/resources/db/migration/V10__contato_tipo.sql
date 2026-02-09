CREATE TABLE contato_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(30) NOT NULL,
  nome VARCHAR(80) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  mascara VARCHAR(60),
  regex_validacao VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_contato_tipo_unique ON contato_tipo (tenant_id, codigo);
CREATE INDEX idx_contato_tipo_tenant ON contato_tipo (tenant_id);
