CREATE TABLE IF NOT EXISTS usuario_empresa_acesso (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  usuario_id VARCHAR(120) NOT NULL,
  empresa_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_empresa_acesso_tenant_usuario_empresa
  ON usuario_empresa_acesso (tenant_id, usuario_id, empresa_id);

CREATE INDEX IF NOT EXISTS idx_usuario_empresa_acesso_tenant_usuario
  ON usuario_empresa_acesso (tenant_id, usuario_id);

CREATE INDEX IF NOT EXISTS idx_usuario_empresa_acesso_tenant_empresa
  ON usuario_empresa_acesso (tenant_id, empresa_id);
