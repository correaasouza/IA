CREATE TABLE locatario (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  data_limite_acesso DATE NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_locatario_data_limite_acesso ON locatario (data_limite_acesso);
