CREATE TABLE usuario_locatario_acesso (
  id BIGSERIAL PRIMARY KEY,
  usuario_id VARCHAR(120) NOT NULL,
  locatario_id BIGINT NOT NULL REFERENCES locatario(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_usuario_locatario_acesso_usuario_locatario
  ON usuario_locatario_acesso (usuario_id, locatario_id);

CREATE INDEX idx_usuario_locatario_acesso_usuario
  ON usuario_locatario_acesso (usuario_id);

INSERT INTO usuario_locatario_acesso (usuario_id, locatario_id, created_at, updated_at)
SELECT DISTINCT u.keycloak_id, u.tenant_id, NOW(), NOW()
FROM usuario u
WHERE u.keycloak_id IS NOT NULL;

