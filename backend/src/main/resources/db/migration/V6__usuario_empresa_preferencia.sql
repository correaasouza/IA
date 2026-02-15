CREATE TABLE usuario_empresa_preferencia (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  usuario_id VARCHAR(120) NOT NULL,
  empresa_padrao_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_usuario_empresa_preferencia_tenant_usuario
  ON usuario_empresa_preferencia (tenant_id, usuario_id);

CREATE INDEX idx_usuario_empresa_preferencia_tenant
  ON usuario_empresa_preferencia (tenant_id);

