CREATE TABLE usuario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  keycloak_id VARCHAR(120) NOT NULL,
  username VARCHAR(120) NOT NULL,
  email VARCHAR(200),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_usuario_tenant ON usuario (tenant_id);
CREATE UNIQUE INDEX idx_usuario_keycloak_id ON usuario (keycloak_id);
