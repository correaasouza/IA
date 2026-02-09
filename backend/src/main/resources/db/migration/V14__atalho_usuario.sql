CREATE TABLE atalho_usuario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id VARCHAR(120) NOT NULL,
  menu_id VARCHAR(120) NOT NULL,
  icon VARCHAR(60),
  ordem INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_atalho_usuario_unique ON atalho_usuario (tenant_id, user_id, menu_id);
CREATE INDEX idx_atalho_usuario_tenant ON atalho_usuario (tenant_id, user_id);
