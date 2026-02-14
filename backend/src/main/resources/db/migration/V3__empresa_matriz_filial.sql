CREATE TABLE empresa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo VARCHAR(10) NOT NULL,
  matriz_id BIGINT,
  razao_social VARCHAR(200) NOT NULL,
  nome_fantasia VARCHAR(200),
  cnpj VARCHAR(14) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_empresa_tipo CHECK (tipo IN ('MATRIZ', 'FILIAL')),
  CONSTRAINT ck_empresa_matriz_relacao CHECK (
    (tipo = 'MATRIZ' AND matriz_id IS NULL) OR
    (tipo = 'FILIAL' AND matriz_id IS NOT NULL)
  ),
  CONSTRAINT ck_empresa_matriz_self CHECK (matriz_id IS NULL OR matriz_id <> id)
);

CREATE UNIQUE INDEX ux_empresa_tenant_cnpj ON empresa (tenant_id, cnpj);
CREATE INDEX idx_empresa_tenant ON empresa (tenant_id);
CREATE INDEX idx_empresa_tenant_tipo ON empresa (tenant_id, tipo);
CREATE INDEX idx_empresa_tenant_matriz ON empresa (tenant_id, matriz_id);

CREATE UNIQUE INDEX ux_empresa_id_tenant ON empresa (id, tenant_id);

ALTER TABLE empresa
  ADD CONSTRAINT fk_empresa_matriz_mesmo_tenant
  FOREIGN KEY (matriz_id, tenant_id)
  REFERENCES empresa (id, tenant_id)
  ON DELETE RESTRICT;
