CREATE TABLE papel (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_papel_tenant_nome ON papel (tenant_id, nome);
CREATE INDEX idx_papel_tenant ON papel (tenant_id);

CREATE TABLE papel_permissao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  papel_id BIGINT NOT NULL REFERENCES papel(id) ON DELETE CASCADE,
  permissao_codigo VARCHAR(80) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_papel_permissao ON papel_permissao (papel_id, permissao_codigo);
CREATE INDEX idx_papel_permissao_tenant ON papel_permissao (tenant_id);

CREATE TABLE usuario_papel (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  usuario_id VARCHAR(120) NOT NULL,
  papel_id BIGINT NOT NULL REFERENCES papel(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_usuario_papel ON usuario_papel (tenant_id, usuario_id, papel_id);
CREATE INDEX idx_usuario_papel_tenant ON usuario_papel (tenant_id);
CREATE INDEX idx_usuario_papel_usuario ON usuario_papel (usuario_id);
