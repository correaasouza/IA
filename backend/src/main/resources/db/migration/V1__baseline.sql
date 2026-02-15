-- ===== BEGIN V1__baseline.sql =====
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
-- ===== END V1__baseline.sql =====

-- ===== BEGIN V2__seed_core.sql =====
INSERT INTO locatario (id, nome, data_limite_acesso, ativo, created_at, updated_at)
VALUES (1, 'Master', '2099-12-31', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
VALUES
  (1, 'CONFIG_EDITOR', 'Configurar colunas e formularios', TRUE),
  (1, 'USUARIO_MANAGE', 'Gerenciar usuarios', TRUE),
  (1, 'PAPEL_MANAGE', 'Gerenciar papeis', TRUE),
  (1, 'RELATORIO_VIEW', 'Visualizar relatorios', TRUE),
  (1, 'ENTIDADE_EDIT', 'Editar entidades', TRUE)
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel (tenant_id, nome, descricao, ativo)
VALUES
  (1, 'MASTER', 'Master do sistema', TRUE),
  (1, 'ADMIN', 'Administrador do locatario', TRUE)
ON CONFLICT (tenant_id, nome) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo)
SELECT p.tenant_id, p.id, pc.codigo
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND pc.codigo IN ('CONFIG_EDITOR', 'USUARIO_MANAGE', 'PAPEL_MANAGE', 'RELATORIO_VIEW')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.papel_id = p.id
      AND pp.permissao_codigo = pc.codigo
  );

-- ===== END V2__seed_core.sql =====

-- ===== BEGIN V3__empresa_matriz_filial.sql =====
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
-- ===== END V3__empresa_matriz_filial.sql =====

-- ===== BEGIN V4__usuario_locatario_acesso.sql =====
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

-- ===== END V4__usuario_locatario_acesso.sql =====

-- ===== BEGIN V5__access_control_policy.sql =====
CREATE TABLE access_control_policy (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  control_key VARCHAR(160) NOT NULL,
  roles_csv VARCHAR(1000) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_access_control_policy_tenant_key
  ON access_control_policy (tenant_id, control_key);

CREATE INDEX idx_access_control_policy_tenant
  ON access_control_policy (tenant_id);

-- ===== END V5__access_control_policy.sql =====

-- ===== BEGIN V6__usuario_empresa_preferencia.sql =====
CREATE TABLE usuario_empresa_preferencia (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  usuario_id VARCHAR(120) NOT NULL,
  empresa_padrao_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_usuario_empresa_preferencia_tenant_usuario
  ON usuario_empresa_preferencia (tenant_id, usuario_id);

CREATE INDEX idx_usuario_empresa_preferencia_tenant
  ON usuario_empresa_preferencia (tenant_id);

-- ===== END V6__usuario_empresa_preferencia.sql =====

-- ===== BEGIN V7__agrupador_empresa_por_configuracao.sql =====
CREATE TABLE agrupador_empresa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_type VARCHAR(80) NOT NULL,
  config_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX ux_agrupador_empresa_config_nome
  ON agrupador_empresa (tenant_id, config_type, config_id, lower(nome));

CREATE UNIQUE INDEX ux_agrupador_empresa_id_scope
  ON agrupador_empresa (id, tenant_id, config_type, config_id);

CREATE INDEX idx_agrupador_empresa_scope
  ON agrupador_empresa (tenant_id, config_type, config_id);

CREATE TABLE agrupador_empresa_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_type VARCHAR(80) NOT NULL,
  config_id BIGINT NOT NULL,
  agrupador_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_agrupador_empresa_item_scope
    FOREIGN KEY (agrupador_id, tenant_id, config_type, config_id)
    REFERENCES agrupador_empresa (id, tenant_id, config_type, config_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_agrupador_empresa_item_empresa_tenant
    FOREIGN KEY (empresa_id, tenant_id)
    REFERENCES empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_agrupador_empresa_item_config_empresa
  ON agrupador_empresa_item (tenant_id, config_type, config_id, empresa_id);

CREATE UNIQUE INDEX ux_agrupador_empresa_item_agrupador_empresa
  ON agrupador_empresa_item (agrupador_id, empresa_id);

CREATE INDEX idx_agrupador_empresa_item_scope
  ON agrupador_empresa_item (tenant_id, config_type, config_id, agrupador_id);
-- ===== END V7__agrupador_empresa_por_configuracao.sql =====

-- ===== BEGIN V8__tipo_entidade.sql =====
CREATE TABLE tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  codigo_seed VARCHAR(40),
  tipo_padrao BOOLEAN NOT NULL DEFAULT FALSE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_tipo_entidade_seed_padrao
    CHECK ((tipo_padrao = FALSE) OR (codigo_seed IS NOT NULL))
);

CREATE UNIQUE INDEX ux_tipo_entidade_tenant_codigo_seed
  ON tipo_entidade (tenant_id, codigo_seed)
  WHERE codigo_seed IS NOT NULL;

CREATE UNIQUE INDEX ux_tipo_entidade_tenant_nome_ativo
  ON tipo_entidade (tenant_id, lower(nome))
  WHERE ativo = TRUE;

CREATE UNIQUE INDEX ux_tipo_entidade_id_tenant
  ON tipo_entidade (id, tenant_id);

CREATE INDEX idx_tipo_entidade_tenant_ativo
  ON tipo_entidade (tenant_id, ativo);

CREATE UNIQUE INDEX ux_agrupador_empresa_id_tenant
  ON agrupador_empresa (id, tenant_id);

CREATE TABLE tipo_entidade_config_agrupador (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  agrupador_id BIGINT NOT NULL,
  obrigar_um_telefone BOOLEAN NOT NULL DEFAULT FALSE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_tipo_ent_cfg_agrup_tipo_tenant
    FOREIGN KEY (tipo_entidade_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_tipo_ent_cfg_agrup_agrupador_tenant
    FOREIGN KEY (agrupador_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_tipo_ent_cfg_agrupador_ativo
  ON tipo_entidade_config_agrupador (tenant_id, tipo_entidade_id, agrupador_id)
  WHERE ativo = TRUE;

CREATE INDEX idx_tipo_ent_cfg_tenant_tipo_ativo
  ON tipo_entidade_config_agrupador (tenant_id, tipo_entidade_id, ativo);

CREATE INDEX idx_tipo_ent_cfg_tenant_agrupador_ativo
  ON tipo_entidade_config_agrupador (tenant_id, agrupador_id, ativo);

INSERT INTO tipo_entidade_config_agrupador (
  tenant_id,
  tipo_entidade_id,
  agrupador_id,
  obrigar_um_telefone,
  ativo
)
SELECT
  a.tenant_id,
  a.config_id AS tipo_entidade_id,
  a.id AS agrupador_id,
  FALSE,
  TRUE
FROM agrupador_empresa a
LEFT JOIN tipo_entidade_config_agrupador c
  ON c.tenant_id = a.tenant_id
  AND c.tipo_entidade_id = a.config_id
  AND c.agrupador_id = a.id
  AND c.ativo = TRUE
WHERE a.config_type = 'TIPO_ENTIDADE'
  AND a.ativo = TRUE
  AND c.id IS NULL;
-- ===== END V8__tipo_entidade.sql =====

