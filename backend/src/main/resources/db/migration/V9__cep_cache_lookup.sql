CREATE TABLE IF NOT EXISTS cep_cache (
  id BIGSERIAL PRIMARY KEY,
  cep VARCHAR(8) NOT NULL,
  logradouro VARCHAR(200),
  bairro VARCHAR(120),
  localidade VARCHAR(120),
  uf VARCHAR(2),
  ibge VARCHAR(10),
  origem VARCHAR(20) NOT NULL DEFAULT 'EXTERNAL',
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cep_cache_cep ON cep_cache (cep);
