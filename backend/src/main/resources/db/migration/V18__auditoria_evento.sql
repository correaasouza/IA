CREATE TABLE auditoria_evento (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo VARCHAR(60) NOT NULL,
  entidade VARCHAR(80) NOT NULL,
  entidade_id VARCHAR(120),
  dados TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_auditoria_evento_tenant ON auditoria_evento (tenant_id);
CREATE INDEX idx_auditoria_evento_tipo ON auditoria_evento (tipo);
