CREATE TABLE permissao_catalogo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(80) NOT NULL,
  label VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_permissao_catalogo ON permissao_catalogo (tenant_id, codigo);
CREATE INDEX idx_permissao_catalogo_tenant ON permissao_catalogo (tenant_id);
