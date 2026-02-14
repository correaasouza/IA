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

CREATE UNIQUE INDEX ux_usuario_keycloak_id ON usuario (keycloak_id);
CREATE UNIQUE INDEX ux_usuario_tenant_username ON usuario (tenant_id, username);
CREATE INDEX idx_usuario_tenant ON usuario (tenant_id);

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

CREATE UNIQUE INDEX ux_permissao_catalogo_tenant_codigo ON permissao_catalogo (tenant_id, codigo);
CREATE INDEX idx_permissao_catalogo_tenant ON permissao_catalogo (tenant_id);

CREATE TABLE papel (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_papel_tenant_nome ON papel (tenant_id, nome);
CREATE INDEX idx_papel_tenant ON papel (tenant_id);

CREATE TABLE papel_permissao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  papel_id BIGINT NOT NULL REFERENCES papel(id) ON DELETE CASCADE,
  permissao_codigo VARCHAR(80) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_papel_permissao_papel_codigo ON papel_permissao (papel_id, permissao_codigo);
CREATE INDEX idx_papel_permissao_tenant ON papel_permissao (tenant_id);

CREATE TABLE usuario_papel (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  usuario_id VARCHAR(120) NOT NULL,
  papel_id BIGINT NOT NULL REFERENCES papel(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_usuario_papel_tenant_usuario_papel ON usuario_papel (tenant_id, usuario_id, papel_id);
CREATE INDEX idx_usuario_papel_tenant_usuario ON usuario_papel (tenant_id, usuario_id);

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

CREATE UNIQUE INDEX ux_atalho_usuario_tenant_user_menu ON atalho_usuario (tenant_id, user_id, menu_id);
CREATE INDEX idx_atalho_usuario_tenant_user_ordem ON atalho_usuario (tenant_id, user_id, ordem);

CREATE TABLE config_coluna (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  screen_id VARCHAR(120) NOT NULL,
  scope_tipo VARCHAR(20) NOT NULL,
  scope_valor VARCHAR(120),
  config_json TEXT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_config_coluna_lookup ON config_coluna (tenant_id, screen_id, scope_tipo, scope_valor);

CREATE TABLE config_formulario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  screen_id VARCHAR(120) NOT NULL,
  scope_tipo VARCHAR(20) NOT NULL,
  scope_valor VARCHAR(120),
  config_json TEXT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_config_form_lookup ON config_formulario (tenant_id, screen_id, scope_tipo, scope_valor);

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

CREATE INDEX idx_auditoria_evento_tenant_tipo ON auditoria_evento (tenant_id, tipo);

CREATE TABLE pessoa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  apelido VARCHAR(200),
  cpf VARCHAR(11),
  cnpj VARCHAR(14),
  id_estrangeiro VARCHAR(40),
  tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_pessoa_tenant_cpf ON pessoa (tenant_id, cpf);
CREATE UNIQUE INDEX ux_pessoa_tenant_cnpj ON pessoa (tenant_id, cnpj);
CREATE UNIQUE INDEX ux_pessoa_tenant_id_estrangeiro ON pessoa (tenant_id, id_estrangeiro);
CREATE INDEX idx_pessoa_tenant ON pessoa (tenant_id);

CREATE TABLE registro_campo_valor (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  campo_definicao_id BIGINT NOT NULL,
  valor_texto VARCHAR(255),
  valor_numero NUMERIC(18,2),
  valor_data DATE,
  valor_booleano BOOLEAN,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_registro_campo_valor_tenant_registro ON registro_campo_valor (tenant_id, registro_entidade_id);
