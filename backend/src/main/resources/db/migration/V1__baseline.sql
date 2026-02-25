-- ===== UNIFIED BASELINE =====
-- Arquivo gerado para base limpa. Consolidacao de todas as migrations anteriores.
-- Fonte: git HEAD com 36 migrations.

-- ===== BEGIN V1__baseline.sql =====
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

-- ===== END V1__baseline.sql =====

-- ===== BEGIN V2__cadastro_entidades.sql =====
-- ===== BEGIN V2__cadastro_entidades.sql =====

CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_ent_cfg_agrupador_id_tenant
  ON tipo_entidade_config_agrupador (id, tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_id_tenant
  ON pessoa (id, tenant_id);

ALTER TABLE pessoa
  ADD COLUMN IF NOT EXISTS tipo_registro VARCHAR(20),
  ADD COLUMN IF NOT EXISTS registro_federal VARCHAR(40),
  ADD COLUMN IF NOT EXISTS registro_federal_normalizado VARCHAR(40);

UPDATE pessoa
SET
  tipo_registro = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN 'CPF'
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN 'CNPJ'
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN 'ID_ESTRANGEIRO'
    ELSE tipo_registro
  END,
  registro_federal = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN cpf
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN cnpj
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN UPPER(BTRIM(id_estrangeiro))
    ELSE registro_federal
  END,
  registro_federal_normalizado = CASE
    WHEN cpf IS NOT NULL AND cpf <> '' THEN cpf
    WHEN cnpj IS NOT NULL AND cnpj <> '' THEN cnpj
    WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN UPPER(BTRIM(id_estrangeiro))
    ELSE registro_federal_normalizado
  END
WHERE tipo_registro IS NULL
   OR registro_federal IS NULL
   OR registro_federal_normalizado IS NULL;

ALTER TABLE pessoa
  ALTER COLUMN tipo_registro SET NOT NULL,
  ALTER COLUMN registro_federal SET NOT NULL,
  ALTER COLUMN registro_federal_normalizado SET NOT NULL;

ALTER TABLE pessoa
  DROP CONSTRAINT IF EXISTS ck_pessoa_tipo_registro;

ALTER TABLE pessoa
  ADD CONSTRAINT ck_pessoa_tipo_registro
    CHECK (tipo_registro IN ('CPF', 'CNPJ', 'ID_ESTRANGEIRO'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_tenant_tipo_registro_federal_norm
  ON pessoa (tenant_id, tipo_registro, registro_federal_normalizado);

CREATE INDEX IF NOT EXISTS idx_pessoa_tenant_tipo_registro_federal_norm
  ON pessoa (tenant_id, tipo_registro, registro_federal_normalizado);

CREATE TABLE registro_entidade_codigo_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_reg_ent_seq_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_registro_entidade_codigo_seq_scope
  ON registro_entidade_codigo_seq (tenant_id, tipo_entidade_config_agrupador_id);

CREATE TABLE grupo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  parent_id BIGINT,
  nome VARCHAR(120) NOT NULL,
  nome_normalizado VARCHAR(120) NOT NULL,
  nivel INTEGER NOT NULL DEFAULT 0,
  path VARCHAR(900) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_grupo_entidade_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_grupo_entidade_id_scope
  ON grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id);

ALTER TABLE grupo_entidade
  ADD CONSTRAINT fk_grupo_entidade_parent_scope
    FOREIGN KEY (parent_id, tenant_id, tipo_entidade_config_agrupador_id)
    REFERENCES grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX ux_grupo_entidade_nome_parent_ativo
  ON grupo_entidade (
    tenant_id,
    tipo_entidade_config_agrupador_id,
    COALESCE(parent_id, 0),
    nome_normalizado
  )
  WHERE ativo = TRUE;

CREATE INDEX idx_grupo_entidade_scope_parent_ordem
  ON grupo_entidade (tenant_id, tipo_entidade_config_agrupador_id, parent_id, ordem);

CREATE INDEX idx_grupo_entidade_scope_path
  ON grupo_entidade (tenant_id, tipo_entidade_config_agrupador_id, path);

CREATE TABLE registro_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_config_agrupador_id BIGINT NOT NULL,
  codigo BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL,
  grupo_entidade_id BIGINT,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_registro_entidade_tipo_cfg_tenant
    FOREIGN KEY (tipo_entidade_config_agrupador_id, tenant_id)
    REFERENCES tipo_entidade_config_agrupador (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_registro_entidade_pessoa_tenant
    FOREIGN KEY (pessoa_id, tenant_id)
    REFERENCES pessoa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_registro_entidade_grupo_scope
    FOREIGN KEY (grupo_entidade_id, tenant_id, tipo_entidade_config_agrupador_id)
    REFERENCES grupo_entidade (id, tenant_id, tipo_entidade_config_agrupador_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_registro_entidade_codigo_scope
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, codigo);

CREATE INDEX idx_registro_entidade_scope_ativo_codigo
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, ativo, codigo);

CREATE INDEX idx_registro_entidade_scope_grupo
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, grupo_entidade_id);

CREATE INDEX idx_registro_entidade_scope_pessoa
  ON registro_entidade (tenant_id, tipo_entidade_config_agrupador_id, pessoa_id);

-- ===== END V2__cadastro_entidades.sql =====
-- ===== END V2__cadastro_entidades.sql =====

-- ===== BEGIN V3__catalog_configuration.sql =====
-- ===== BEGIN V3__catalog_configuration.sql =====

CREATE TABLE IF NOT EXISTS catalog_configuration (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  numbering_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICA',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_configuration_type
    CHECK (type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_catalog_configuration_numbering_mode
    CHECK (numbering_mode IN ('AUTOMATICA', 'MANUAL'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_configuration_tenant_type
  ON catalog_configuration (tenant_id, type);

-- ===== END V3__catalog_configuration.sql =====
-- ===== END V3__catalog_configuration.sql =====

-- ===== BEGIN V4__catalog_configuration_group.sql =====
-- ===== BEGIN V4__catalog_configuration_group.sql =====

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_configuration_id_tenant
  ON catalog_configuration (id, tenant_id);

CREATE TABLE IF NOT EXISTS catalog_configuration_by_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_id BIGINT NOT NULL,
  numbering_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICA',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_cfg_group_numbering_mode
    CHECK (numbering_mode IN ('AUTOMATICA', 'MANUAL')),
  CONSTRAINT fk_catalog_cfg_group_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_cfg_group_agrupador_scope
    FOREIGN KEY (agrupador_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_cfg_group_active
  ON catalog_configuration_by_group (tenant_id, catalog_configuration_id, agrupador_id, active);

CREATE INDEX IF NOT EXISTS idx_catalog_cfg_group_scope
  ON catalog_configuration_by_group (tenant_id, catalog_configuration_id, active);

-- ===== END V4__catalog_configuration_group.sql =====
-- ===== END V4__catalog_configuration_group.sql =====

-- ===== BEGIN V5__catalog_groups.sql =====
-- ===== BEGIN V5__catalog_groups.sql =====

CREATE TABLE IF NOT EXISTS catalog_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  parent_id BIGINT,
  nome VARCHAR(120) NOT NULL,
  nome_normalizado VARCHAR(120) NOT NULL,
  nivel INTEGER NOT NULL DEFAULT 0,
  path VARCHAR(900) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_group_nivel CHECK (nivel >= 0),
  CONSTRAINT ck_catalog_group_ordem CHECK (ordem >= 0),
  CONSTRAINT fk_catalog_group_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_group_id_scope
  ON catalog_group (id, tenant_id, catalog_configuration_id);

ALTER TABLE catalog_group
  ADD CONSTRAINT fk_catalog_group_parent_scope
    FOREIGN KEY (parent_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_group_nome_parent_ativo
  ON catalog_group (
    tenant_id,
    catalog_configuration_id,
    COALESCE(parent_id, 0),
    nome_normalizado,
    ativo
  );

CREATE INDEX IF NOT EXISTS idx_catalog_group_scope_parent_ordem
  ON catalog_group (tenant_id, catalog_configuration_id, parent_id, ordem);

CREATE INDEX IF NOT EXISTS idx_catalog_group_scope_path
  ON catalog_group (tenant_id, catalog_configuration_id, path);

-- ===== END V5__catalog_groups.sql =====
-- ===== END V5__catalog_groups.sql =====

-- ===== BEGIN V6__catalog_items.sql =====
-- ===== BEGIN V6__catalog_items.sql =====

CREATE TABLE IF NOT EXISTS catalog_product (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  catalog_group_id BIGINT,
  codigo BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_product_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_product_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_product_group_scope
    FOREIGN KEY (catalog_group_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_product_codigo_scope
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_product_scope_ativo_codigo
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, ativo, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_product_scope_group
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, catalog_group_id);

CREATE TABLE IF NOT EXISTS catalog_service_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  catalog_group_id BIGINT,
  codigo BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_service_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_service_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_service_group_scope
    FOREIGN KEY (catalog_group_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_service_item_codigo_scope
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_service_scope_ativo_codigo
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, ativo, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_service_scope_group
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, catalog_group_id);

-- ===== END V6__catalog_items.sql =====
-- ===== END V6__catalog_items.sql =====

-- ===== BEGIN V7__catalog_item_code_seq.sql =====
-- ===== BEGIN V7__catalog_item_code_seq.sql =====

CREATE TABLE IF NOT EXISTS catalog_item_code_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_item_code_seq_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_item_code_seq_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_item_code_seq_scope
  ON catalog_item_code_seq (tenant_id, catalog_configuration_id, agrupador_empresa_id);

-- ===== END V7__catalog_item_code_seq.sql =====
-- ===== END V7__catalog_item_code_seq.sql =====

-- ===== BEGIN V8__catalog_stock_type.sql =====
-- ===== BEGIN V8__catalog_stock_type.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_type (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 1,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_type_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_stock_type_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_type_scope_codigo_active
  ON catalog_stock_type (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo, active);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_type_scope
  ON catalog_stock_type (tenant_id, catalog_configuration_id, agrupador_empresa_id, active, ordem);

-- ===== END V8__catalog_stock_type.sql =====
-- ===== END V8__catalog_stock_type.sql =====

-- ===== BEGIN V9__catalog_movement.sql =====
-- ===== BEGIN V9__catalog_movement.sql =====

CREATE TABLE IF NOT EXISTS catalog_movement (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalogo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  origem_movimentacao_tipo VARCHAR(40) NOT NULL,
  origem_movimentacao_codigo VARCHAR(120),
  origem_movimento_item_codigo VARCHAR(120),
  data_hora_movimentacao TIMESTAMP NOT NULL DEFAULT NOW(),
  observacao VARCHAR(255),
  idempotency_key VARCHAR(180) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_movement_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_movement_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_movement_idempotency
  ON catalog_movement (tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_catalog_data
  ON catalog_movement (tenant_id, catalog_type, catalogo_id, data_hora_movimentacao DESC);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_origem
  ON catalog_movement (tenant_id, origem_movimentacao_tipo, origem_movimentacao_codigo);

CREATE TABLE IF NOT EXISTS catalog_movement_line (
  id BIGSERIAL PRIMARY KEY,
  movement_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  metric_type VARCHAR(20) NOT NULL,
  estoque_tipo_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  before_value NUMERIC(19,6) NOT NULL,
  delta NUMERIC(19,6) NOT NULL,
  after_value NUMERIC(19,6) NOT NULL,
  CONSTRAINT fk_catalog_movement_line_movement
    FOREIGN KEY (movement_id)
    REFERENCES catalog_movement (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_movement_line_stock_type
    FOREIGN KEY (estoque_tipo_id)
    REFERENCES catalog_stock_type (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_movement_line_filial
    FOREIGN KEY (filial_id)
    REFERENCES empresa (id)
    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_line_movement
  ON catalog_movement_line (movement_id);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_line_scope
  ON catalog_movement_line (tenant_id, agrupador_empresa_id, estoque_tipo_id, filial_id);

-- ===== END V9__catalog_movement.sql =====
-- ===== END V9__catalog_movement.sql =====

-- ===== BEGIN V10__catalog_stock_balance.sql =====
-- ===== BEGIN V10__catalog_stock_balance.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_balance (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalogo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  estoque_tipo_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  quantidade_atual NUMERIC(19,6) NOT NULL DEFAULT 0,
  preco_atual NUMERIC(19,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_balance_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_stock_balance_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_stock_balance_stock_type
    FOREIGN KEY (estoque_tipo_id)
    REFERENCES catalog_stock_type (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_stock_balance_filial
    FOREIGN KEY (filial_id)
    REFERENCES empresa (id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_balance_scope
  ON catalog_stock_balance (tenant_id, catalog_type, catalogo_id, agrupador_empresa_id, estoque_tipo_id, filial_id);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_balance_catalog
  ON catalog_stock_balance (tenant_id, catalog_type, catalogo_id);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_balance_group_stock
  ON catalog_stock_balance (tenant_id, catalog_configuration_id, agrupador_empresa_id, estoque_tipo_id);

-- ===== END V10__catalog_stock_balance.sql =====
-- ===== END V10__catalog_stock_balance.sql =====

-- ===== BEGIN V11__catalog_stock_adjustment.sql =====
-- ===== BEGIN V11__catalog_stock_adjustment.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_adjustment (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 1,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_adjustment_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_adjustment_scope_codigo_active
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, codigo, active);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_adjustment_scope
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, active, ordem);

-- ===== END V11__catalog_stock_adjustment.sql =====

-- ===== END V11__catalog_stock_adjustment.sql =====

-- ===== BEGIN V12__catalog_stock_adjustment_type_scope.sql =====
-- ===== BEGIN V12__catalog_stock_adjustment_type_scope.sql =====

ALTER TABLE catalog_stock_adjustment
  ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'ENTRADA',
  ADD COLUMN IF NOT EXISTS estoque_origem_agrupador_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_origem_tipo_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_origem_filial_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_agrupador_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_tipo_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_destino_filial_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'ck_catalog_stock_adjustment_tipo'
  ) THEN
    ALTER TABLE catalog_stock_adjustment
      ADD CONSTRAINT ck_catalog_stock_adjustment_tipo
      CHECK (tipo IN ('ENTRADA', 'SAIDA', 'TRANSFERENCIA'));
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_catalog_stock_adjustment_type
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, tipo, active, ordem);

-- ===== END V12__catalog_stock_adjustment_type_scope.sql =====

-- ===== END V12__catalog_stock_adjustment_type_scope.sql =====

-- ===== BEGIN V13__catalog_stock_adjustment_codigo_tenant.sql =====
-- ===== BEGIN V13__catalog_stock_adjustment_codigo_tenant.sql =====

DROP INDEX IF EXISTS ux_catalog_stock_adjustment_scope_codigo_active;

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_adjustment_tenant_codigo
  ON catalog_stock_adjustment (tenant_id, codigo);

-- ===== END V13__catalog_stock_adjustment_codigo_tenant.sql =====
-- ===== END V13__catalog_stock_adjustment_codigo_tenant.sql =====

-- ===== BEGIN V14__movimento_configuracao.sql =====
-- ===== BEGIN V14__movimento_configuracao.sql =====

CREATE TABLE IF NOT EXISTS movimento_config (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_tipo VARCHAR(80) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  prioridade INTEGER NOT NULL DEFAULT 100,
  contexto_key VARCHAR(120),
  tipo_entidade_padrao_id BIGINT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_movimento_config_prioridade_non_negative CHECK (prioridade >= 0),
  CONSTRAINT fk_movimento_config_tipo_padrao_tenant
    FOREIGN KEY (tipo_entidade_padrao_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_id_tenant
  ON movimento_config (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tenant_tipo_ativo
  ON movimento_config (tenant_id, movimento_tipo, ativo);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tenant_contexto_prioridade
  ON movimento_config (tenant_id, movimento_tipo, contexto_key, prioridade);

CREATE TABLE IF NOT EXISTS movimento_config_empresa (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_movimento_config_empresa_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_movimento_config_empresa_empresa_tenant
    FOREIGN KEY (empresa_id, tenant_id)
    REFERENCES empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_empresa_scope
  ON movimento_config_empresa (tenant_id, movimento_config_id, empresa_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_empresa_lookup
  ON movimento_config_empresa (tenant_id, empresa_id, movimento_config_id);

CREATE TABLE IF NOT EXISTS movimento_config_tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_movimento_config_tipo_entidade_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_movimento_config_tipo_entidade_tenant
    FOREIGN KEY (tipo_entidade_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_config_tipo_entidade_scope
  ON movimento_config_tipo_entidade (tenant_id, movimento_config_id, tipo_entidade_id);

CREATE INDEX IF NOT EXISTS idx_movimento_config_tipo_entidade_lookup
  ON movimento_config_tipo_entidade (tenant_id, tipo_entidade_id, movimento_config_id);

-- ===== END V14__movimento_configuracao.sql =====
-- ===== END V14__movimento_configuracao.sql =====

-- ===== BEGIN V15__movimento_config_tipo_entidade_padrao_nullable.sql =====
-- ===== BEGIN V15__movimento_config_tipo_entidade_padrao_nullable.sql =====

ALTER TABLE movimento_config
  ALTER COLUMN tipo_entidade_padrao_id DROP NOT NULL;

-- ===== END V15__movimento_config_tipo_entidade_padrao_nullable.sql =====
-- ===== END V15__movimento_config_tipo_entidade_padrao_nullable.sql =====

-- ===== BEGIN V16__movimento_estoque.sql =====
-- ===== BEGIN V16__movimento_estoque.sql =====

CREATE TABLE IF NOT EXISTS movimento_estoque (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  tipo_movimento VARCHAR(80) NOT NULL,
  data_movimento DATE,
  status VARCHAR(80),
  nome VARCHAR(120) NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  tipo_entidade_padrao_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_movimento_estoque_tipo
    CHECK (tipo_movimento = 'MOVIMENTO_ESTOQUE'),
  CONSTRAINT fk_movimento_estoque_empresa_tenant
    FOREIGN KEY (empresa_id, tenant_id)
    REFERENCES empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimento_estoque_config_tenant
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_movimento_estoque_tipo_entidade_tenant
    FOREIGN KEY (tipo_entidade_padrao_id, tenant_id)
    REFERENCES tipo_entidade (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_id_tenant
  ON movimento_estoque (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_data
  ON movimento_estoque (tenant_id, empresa_id, data_movimento DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_nome
  ON movimento_estoque (tenant_id, nome);

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_nome
  ON movimento_estoque (tenant_id, empresa_id, nome);

-- ===== END V16__movimento_estoque.sql =====
-- ===== END V16__movimento_estoque.sql =====

-- ===== BEGIN V17__permissao_movimento_estoque_operar.sql =====
-- ===== BEGIN V17__permissao_movimento_estoque_operar.sql =====

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES (1, 'MOVIMENTO_ESTOQUE_OPERAR', 'Operar movimento de estoque', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, 'MOVIMENTO_ESTOQUE_OPERAR', NOW(), NOW()
FROM papel p
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = 'MOVIMENTO_ESTOQUE_OPERAR'
  );

-- ===== END V17__permissao_movimento_estoque_operar.sql =====
-- ===== END V17__permissao_movimento_estoque_operar.sql =====

-- ===== BEGIN V18__movimento_estoque_drop_data_movimento.sql =====
-- ===== BEGIN V18__movimento_estoque_drop_data_movimento.sql =====

DROP INDEX IF EXISTS idx_movimento_estoque_tenant_empresa_data;

ALTER TABLE movimento_estoque
  DROP COLUMN IF EXISTS data_movimento;

CREATE INDEX IF NOT EXISTS idx_movimento_estoque_tenant_empresa_id
  ON movimento_estoque (tenant_id, empresa_id, id DESC);

-- ===== END V18__movimento_estoque_drop_data_movimento.sql =====
-- ===== END V18__movimento_estoque_drop_data_movimento.sql =====

-- ===== BEGIN V19__movimento_item_tipo.sql =====
-- ===== BEGIN V19__movimento_item_tipo.sql =====

CREATE TABLE IF NOT EXISTS movimento_item_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_movimento_item_tipo_catalog_type
    CHECK (catalog_type IN ('PRODUCTS', 'SERVICES'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_item_tipo_tenant_nome
  ON movimento_item_tipo (tenant_id, nome);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_item_tipo_id_tenant
  ON movimento_item_tipo (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_item_tipo_lookup
  ON movimento_item_tipo (tenant_id, catalog_type, ativo, nome);

-- ===== END V19__movimento_item_tipo.sql =====
-- ===== END V19__movimento_item_tipo.sql =====

-- ===== BEGIN V20__movimento_config_item_tipo.sql =====
-- ===== BEGIN V20__movimento_config_item_tipo.sql =====

CREATE TABLE IF NOT EXISTS movimento_config_item_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  movimento_item_tipo_id BIGINT NOT NULL,
  cobrar BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_mov_config_item_tipo_config_scope
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_mov_config_item_tipo_tipo_scope
    FOREIGN KEY (movimento_item_tipo_id, tenant_id)
    REFERENCES movimento_item_tipo (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_mov_config_item_tipo_scope
  ON movimento_config_item_tipo (tenant_id, movimento_config_id, movimento_item_tipo_id);

CREATE INDEX IF NOT EXISTS idx_mov_config_item_tipo_config
  ON movimento_config_item_tipo (tenant_id, movimento_config_id);

-- ===== END V20__movimento_config_item_tipo.sql =====
-- ===== END V20__movimento_config_item_tipo.sql =====

-- ===== BEGIN V21__movimento_estoque_item.sql =====
-- ===== BEGIN V21__movimento_estoque_item.sql =====

CREATE TABLE IF NOT EXISTS movimento_estoque_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_estoque_id BIGINT NOT NULL,
  movimento_item_tipo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  catalog_codigo_snapshot BIGINT NOT NULL,
  catalog_nome_snapshot VARCHAR(200) NOT NULL,
  quantidade NUMERIC(19,6) NOT NULL DEFAULT 0,
  valor_unitario NUMERIC(19,6) NOT NULL DEFAULT 0,
  valor_total NUMERIC(19,6) NOT NULL DEFAULT 0,
  cobrar BOOLEAN NOT NULL DEFAULT TRUE,
  ordem INTEGER NOT NULL DEFAULT 0,
  observacao VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_mov_estoque_item_catalog_type
    CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_mov_estoque_item_quantidade_non_negative
    CHECK (quantidade >= 0),
  CONSTRAINT ck_mov_estoque_item_preco_non_negative
    CHECK (valor_unitario >= 0 AND valor_total >= 0),
  CONSTRAINT ck_mov_estoque_item_ordem_non_negative
    CHECK (ordem >= 0),
  CONSTRAINT ck_mov_estoque_item_cobrar_zero
    CHECK (cobrar = TRUE OR (valor_unitario = 0 AND valor_total = 0)),
  CONSTRAINT fk_mov_estoque_item_movimento_scope
    FOREIGN KEY (movimento_estoque_id, tenant_id)
    REFERENCES movimento_estoque (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_mov_estoque_item_tipo_scope
    FOREIGN KEY (movimento_item_tipo_id, tenant_id)
    REFERENCES movimento_item_tipo (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_movimento
  ON movimento_estoque_item (tenant_id, movimento_estoque_id, ordem, id);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_tipo
  ON movimento_estoque_item (tenant_id, movimento_item_tipo_id);

-- ===== END V21__movimento_estoque_item.sql =====
-- ===== END V21__movimento_estoque_item.sql =====

-- ===== BEGIN V22__permissoes_movimento_itens.sql =====
-- ===== BEGIN V22__permissoes_movimento_itens.sql =====

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES
  (1, 'MOVIMENTO_ITEM_CONFIGURAR', 'Configurar tipos de itens de movimento', TRUE, NOW(), NOW()),
  (1, 'MOVIMENTO_ESTOQUE_ITEM_OPERAR', 'Operar itens no movimento de estoque', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, x.codigo, NOW(), NOW()
FROM papel p
JOIN (
  SELECT 'MOVIMENTO_ITEM_CONFIGURAR' AS codigo
  UNION ALL
  SELECT 'MOVIMENTO_ESTOQUE_ITEM_OPERAR' AS codigo
) x ON TRUE
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = x.codigo
  );

-- ===== END V22__permissoes_movimento_itens.sql =====
-- ===== END V22__permissoes_movimento_itens.sql =====

-- ===== BEGIN V23__seed_movimento_estoque_200.sql =====
-- ===== BEGIN V23__seed_movimento_estoque_200.sql =====
-- Seed tecnico para ambiente local: garante 200 movimentos de estoque no tenant 1.
-- Idempotente por prefixo de nome, evitando duplicacao em reexecucoes manuais.

DO $$
DECLARE
  v_tenant_id BIGINT := 1;
  v_empresa_id BIGINT;
  v_config_id BIGINT;
  v_tipo_entidade_padrao_id BIGINT;
  v_existing_count INTEGER := 0;
  v_to_insert INTEGER := 0;
BEGIN
  SELECT mce.empresa_id, mc.id, mc.tipo_entidade_padrao_id
    INTO v_empresa_id, v_config_id, v_tipo_entidade_padrao_id
  FROM movimento_config mc
  JOIN movimento_config_empresa mce
    ON mce.movimento_config_id = mc.id
   AND mce.tenant_id = mc.tenant_id
  WHERE mc.tenant_id = v_tenant_id
    AND mc.ativo = TRUE
    AND mc.movimento_tipo = 'MOVIMENTO_ESTOQUE'
  ORDER BY mc.prioridade DESC, mc.updated_at DESC, mc.id DESC
  LIMIT 1;

  IF v_empresa_id IS NULL OR v_config_id IS NULL THEN
    RAISE NOTICE 'V23 seed ignorado: sem configuracao MOVIMENTO_ESTOQUE ativa para tenant %.', v_tenant_id;
    RETURN;
  END IF;

  SELECT COUNT(*)
    INTO v_existing_count
  FROM movimento_estoque me
  WHERE me.tenant_id = v_tenant_id
    AND me.empresa_id = v_empresa_id
    AND me.nome LIKE 'SEED MOV ESTOQUE %';

  v_to_insert := GREATEST(0, 200 - v_existing_count);

  IF v_to_insert = 0 THEN
    RAISE NOTICE 'V23 seed: ja existem 200 movimentos SEED para tenant %, empresa %.', v_tenant_id, v_empresa_id;
    RETURN;
  END IF;

  INSERT INTO movimento_estoque (
    tenant_id,
    empresa_id,
    tipo_movimento,
    status,
    nome,
    movimento_config_id,
    tipo_entidade_padrao_id,
    version,
    created_by,
    updated_by
  )
  SELECT
    v_tenant_id,
    v_empresa_id,
    'MOVIMENTO_ESTOQUE',
    'ABERTO',
    'SEED MOV ESTOQUE ' || LPAD((v_existing_count + gs)::TEXT, 4, '0'),
    v_config_id,
    v_tipo_entidade_padrao_id,
    0,
    'flyway-seed',
    'flyway-seed'
  FROM generate_series(1, v_to_insert) gs;

  RAISE NOTICE 'V23 seed: inseridos % movimentos para tenant %, empresa %.', v_to_insert, v_tenant_id, v_empresa_id;
END $$;

-- ===== END V23__seed_movimento_estoque_200.sql =====
-- ===== END V23__seed_movimento_estoque_200.sql =====

-- ===== BEGIN V24__seed_movimento_estoque_itens.sql =====
-- ===== BEGIN V24__seed_movimento_estoque_itens.sql =====
-- Seed tecnico: adiciona itens nos movimentos "SEED MOV ESTOQUE ####" (tenant 1).
-- Idempotente: somente movimentos seed sem itens recebem carga.

DO $$
DECLARE
  v_tenant_id BIGINT := 1;
  v_seed_without_items INTEGER := 0;
  v_inserted_items INTEGER := 0;
BEGIN
  SELECT COUNT(*)
    INTO v_seed_without_items
  FROM movimento_estoque me
  WHERE me.tenant_id = v_tenant_id
    AND me.nome LIKE 'SEED MOV ESTOQUE %'
    AND NOT EXISTS (
      SELECT 1
      FROM movimento_estoque_item mei
      WHERE mei.tenant_id = me.tenant_id
        AND mei.movimento_estoque_id = me.id
    );

  IF v_seed_without_items = 0 THEN
    RAISE NOTICE 'V24 seed: nenhum movimento seed sem itens para tenant %.', v_tenant_id;
    RETURN;
  END IF;

  WITH seed_movimentos AS (
    SELECT me.id, me.tenant_id, me.movimento_config_id
    FROM movimento_estoque me
    WHERE me.tenant_id = v_tenant_id
      AND me.nome LIKE 'SEED MOV ESTOQUE %'
      AND NOT EXISTS (
        SELECT 1
        FROM movimento_estoque_item mei
        WHERE mei.tenant_id = me.tenant_id
          AND mei.movimento_estoque_id = me.id
      )
  ),
  tipo_por_movimento AS (
    SELECT
      sm.id AS movimento_estoque_id,
      sm.tenant_id,
      mci.movimento_item_tipo_id,
      mit.catalog_type,
      mci.cobrar
    FROM seed_movimentos sm
    JOIN LATERAL (
      SELECT mci.movimento_item_tipo_id, mci.cobrar
      FROM movimento_config_item_tipo mci
      WHERE mci.tenant_id = sm.tenant_id
        AND mci.movimento_config_id = sm.movimento_config_id
      ORDER BY mci.id
      LIMIT 1
    ) mci ON TRUE
    JOIN movimento_item_tipo mit
      ON mit.id = mci.movimento_item_tipo_id
     AND mit.tenant_id = sm.tenant_id
     AND mit.ativo = TRUE
  ),
  catalogo_por_movimento AS (
    SELECT
      tpm.movimento_estoque_id,
      tpm.tenant_id,
      tpm.movimento_item_tipo_id,
      tpm.catalog_type,
      tpm.cobrar,
      COALESCE(p.catalog_item_id, s.catalog_item_id) AS catalog_item_id,
      COALESCE(p.codigo_snapshot, s.codigo_snapshot) AS codigo_snapshot,
      COALESCE(p.nome_snapshot, s.nome_snapshot) AS nome_snapshot
    FROM tipo_por_movimento tpm
    LEFT JOIN LATERAL (
      SELECT p.id AS catalog_item_id, p.codigo AS codigo_snapshot, p.nome AS nome_snapshot
      FROM catalog_product p
      WHERE tpm.catalog_type = 'PRODUCTS'
        AND p.tenant_id = tpm.tenant_id
        AND p.ativo = TRUE
      ORDER BY p.id
      LIMIT 1
    ) p ON TRUE
    LEFT JOIN LATERAL (
      SELECT s.id AS catalog_item_id, s.codigo AS codigo_snapshot, s.nome AS nome_snapshot
      FROM catalog_service_item s
      WHERE tpm.catalog_type = 'SERVICES'
        AND s.tenant_id = tpm.tenant_id
        AND s.ativo = TRUE
      ORDER BY s.id
      LIMIT 1
    ) s ON TRUE
    WHERE COALESCE(p.catalog_item_id, s.catalog_item_id) IS NOT NULL
  ),
  itens_para_inserir AS (
    SELECT
      cpm.tenant_id,
      cpm.movimento_estoque_id,
      cpm.movimento_item_tipo_id,
      cpm.catalog_type,
      cpm.catalog_item_id,
      cpm.codigo_snapshot,
      cpm.nome_snapshot,
      cpm.cobrar,
      gs.ordem
    FROM catalogo_por_movimento cpm
    CROSS JOIN LATERAL (
      SELECT 0 AS ordem
      UNION ALL
      SELECT 1 AS ordem
    ) gs
  )
  INSERT INTO movimento_estoque_item (
    tenant_id,
    movimento_estoque_id,
    movimento_item_tipo_id,
    catalog_type,
    catalog_item_id,
    catalog_codigo_snapshot,
    catalog_nome_snapshot,
    quantidade,
    valor_unitario,
    valor_total,
    cobrar,
    ordem,
    observacao,
    created_by,
    updated_by
  )
  SELECT
    i.tenant_id,
    i.movimento_estoque_id,
    i.movimento_item_tipo_id,
    i.catalog_type,
    i.catalog_item_id,
    i.codigo_snapshot,
    i.nome_snapshot,
    CASE WHEN i.ordem = 0 THEN 1.000000 ELSE 2.000000 END::numeric(19,6),
    CASE WHEN i.cobrar THEN (CASE WHEN i.ordem = 0 THEN 100.000000 ELSE 150.000000 END)::numeric(19,6) ELSE 0::numeric(19,6) END,
    CASE WHEN i.cobrar THEN (CASE WHEN i.ordem = 0 THEN 100.000000 ELSE 300.000000 END)::numeric(19,6) ELSE 0::numeric(19,6) END,
    i.cobrar,
    i.ordem,
    'Item seed',
    'flyway-seed',
    'flyway-seed'
  FROM itens_para_inserir i;

  GET DIAGNOSTICS v_inserted_items = ROW_COUNT;
  RAISE NOTICE 'V24 seed: inseridos % itens seed para tenant %.', v_inserted_items, v_tenant_id;
END $$;

-- ===== END V24__seed_movimento_estoque_itens.sql =====
-- ===== END V24__seed_movimento_estoque_itens.sql =====

-- ===== BEGIN V25__workflow_mvp.sql =====
-- ===== BEGIN V25__workflow_mvp.sql =====

ALTER TABLE movimento_estoque
  ADD COLUMN IF NOT EXISTS stock_adjustment_id BIGINT;

ALTER TABLE movimento_estoque
  ADD CONSTRAINT fk_movimento_estoque_stock_adjustment
  FOREIGN KEY (stock_adjustment_id)
  REFERENCES catalog_stock_adjustment (id)
  ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_mov_estoque_tenant_adjustment
  ON movimento_estoque (tenant_id, stock_adjustment_id);

ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS status VARCHAR(80),
  ADD COLUMN IF NOT EXISTS estoque_movimentado BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS estoque_movimentado_em TIMESTAMP,
  ADD COLUMN IF NOT EXISTS estoque_movimentado_por VARCHAR(120),
  ADD COLUMN IF NOT EXISTS estoque_movimentacao_id BIGINT,
  ADD COLUMN IF NOT EXISTS estoque_movimentacao_chave VARCHAR(180);

CREATE INDEX IF NOT EXISTS idx_mov_item_tenant_status
  ON movimento_estoque_item (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_mov_item_tenant_mov_estoque
  ON movimento_estoque_item (tenant_id, movimento_estoque_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_mov_item_tenant_mov_chave
  ON movimento_estoque_item (tenant_id, estoque_movimentacao_chave)
  WHERE estoque_movimentacao_chave IS NOT NULL;

CREATE TABLE IF NOT EXISTS workflow_definition (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  origin VARCHAR(60) NOT NULL,
  name VARCHAR(120) NOT NULL,
  version_num INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  description VARCHAR(255),
  layout_json TEXT,
  published_at TIMESTAMP,
  published_by VARCHAR(120),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  entity_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_workflow_definition_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
  CONSTRAINT ck_workflow_definition_origin
    CHECK (origin IN ('MOVIMENTO_ESTOQUE', 'ITEM_MOVIMENTO_ESTOQUE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_def_tenant_origin_version
  ON workflow_definition (tenant_id, origin, version_num);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_def_tenant_origin_published
  ON workflow_definition (tenant_id, origin)
  WHERE status = 'PUBLISHED' AND active = TRUE;

CREATE INDEX IF NOT EXISTS idx_workflow_def_tenant_origin_status
  ON workflow_definition (tenant_id, origin, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS workflow_state (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL,
  state_key VARCHAR(80) NOT NULL,
  name VARCHAR(120) NOT NULL,
  color VARCHAR(20),
  is_initial BOOLEAN NOT NULL DEFAULT FALSE,
  is_final BOOLEAN NOT NULL DEFAULT FALSE,
  ui_x INTEGER NOT NULL DEFAULT 0,
  ui_y INTEGER NOT NULL DEFAULT 0,
  metadata_json TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_workflow_state_definition
    FOREIGN KEY (definition_id)
    REFERENCES workflow_definition (id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_state_def_key
  ON workflow_state (definition_id, state_key);

CREATE INDEX IF NOT EXISTS idx_workflow_state_def
  ON workflow_state (definition_id);

CREATE INDEX IF NOT EXISTS idx_workflow_state_tenant_def_initial
  ON workflow_state (tenant_id, definition_id, is_initial);

CREATE TABLE IF NOT EXISTS workflow_transition (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL,
  transition_key VARCHAR(80) NOT NULL,
  name VARCHAR(120) NOT NULL,
  from_state_id BIGINT NOT NULL,
  to_state_id BIGINT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  priority INTEGER NOT NULL DEFAULT 100,
  permissions_json TEXT,
  conditions_json TEXT,
  actions_json TEXT,
  ui_meta_json TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_workflow_transition_definition
    FOREIGN KEY (definition_id)
    REFERENCES workflow_definition (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_workflow_transition_from
    FOREIGN KEY (from_state_id)
    REFERENCES workflow_state (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_workflow_transition_to
    FOREIGN KEY (to_state_id)
    REFERENCES workflow_state (id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_transition_def_key
  ON workflow_transition (definition_id, transition_key);

CREATE INDEX IF NOT EXISTS idx_workflow_transition_def_from
  ON workflow_transition (definition_id, from_state_id, enabled);

CREATE INDEX IF NOT EXISTS idx_workflow_transition_def_to
  ON workflow_transition (definition_id, to_state_id);

CREATE TABLE IF NOT EXISTS workflow_instance (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  origin VARCHAR(60) NOT NULL,
  entity_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL,
  definition_version INTEGER NOT NULL,
  current_state_id BIGINT NOT NULL,
  current_state_key VARCHAR(80) NOT NULL,
  last_transition_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_workflow_instance_origin
    CHECK (origin IN ('MOVIMENTO_ESTOQUE', 'ITEM_MOVIMENTO_ESTOQUE')),
  CONSTRAINT fk_workflow_instance_definition
    FOREIGN KEY (definition_id)
    REFERENCES workflow_definition (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_workflow_instance_state
    FOREIGN KEY (current_state_id)
    REFERENCES workflow_state (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_workflow_instance_transition
    FOREIGN KEY (last_transition_id)
    REFERENCES workflow_transition (id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_instance_origin_entity
  ON workflow_instance (tenant_id, origin, entity_id);

CREATE INDEX IF NOT EXISTS idx_workflow_instance_tenant_origin_state
  ON workflow_instance (tenant_id, origin, current_state_key);

CREATE INDEX IF NOT EXISTS idx_workflow_instance_tenant_origin_updated
  ON workflow_instance (tenant_id, origin, updated_at DESC);

CREATE TABLE IF NOT EXISTS workflow_history (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  instance_id BIGINT NOT NULL,
  origin VARCHAR(60) NOT NULL,
  entity_id BIGINT NOT NULL,
  from_state_key VARCHAR(80) NOT NULL,
  to_state_key VARCHAR(80) NOT NULL,
  transition_key VARCHAR(80) NOT NULL,
  triggered_by VARCHAR(120) NOT NULL,
  triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
  notes VARCHAR(500),
  action_results_json TEXT,
  success BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_workflow_history_instance
    FOREIGN KEY (instance_id)
    REFERENCES workflow_instance (id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workflow_hist_instance_time
  ON workflow_history (instance_id, triggered_at DESC);

CREATE INDEX IF NOT EXISTS idx_workflow_hist_tenant_origin_entity_time
  ON workflow_history (tenant_id, origin, entity_id, triggered_at DESC);

CREATE TABLE IF NOT EXISTS workflow_action_execution (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  instance_id BIGINT NOT NULL,
  history_id BIGINT,
  action_type VARCHAR(60) NOT NULL,
  execution_key VARCHAR(180) NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempt_count INTEGER NOT NULL DEFAULT 1,
  request_json TEXT,
  result_json TEXT,
  error_message VARCHAR(1000),
  executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  executed_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_workflow_action_execution_status
    CHECK (status IN ('STARTED', 'SUCCESS', 'FAILED', 'PENDING')),
  CONSTRAINT fk_workflow_action_execution_instance
    FOREIGN KEY (instance_id)
    REFERENCES workflow_instance (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_workflow_action_execution_history
    FOREIGN KEY (history_id)
    REFERENCES workflow_history (id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wf_action_exec_key
  ON workflow_action_execution (tenant_id, execution_key);

CREATE INDEX IF NOT EXISTS idx_wf_action_exec_pending
  ON workflow_action_execution (tenant_id, status, executed_at);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES
  (1, 'WORKFLOW_CONFIGURAR', 'Configurar workflows', TRUE, NOW(), NOW()),
  (1, 'WORKFLOW_TRANSICIONAR', 'Executar transicoes de workflow', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, x.codigo, NOW(), NOW()
FROM papel p
JOIN (
  SELECT 'WORKFLOW_CONFIGURAR' AS codigo
  UNION ALL
  SELECT 'WORKFLOW_TRANSICIONAR' AS codigo
) x ON TRUE
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = x.codigo
  );

-- ===== END V25__workflow_mvp.sql =====
-- ===== END V25__workflow_mvp.sql =====

-- ===== BEGIN V26__workflow_remove_permissions_conditions.sql =====
ALTER TABLE workflow_transition
  DROP COLUMN IF EXISTS permissions_json,
  DROP COLUMN IF EXISTS conditions_json;
-- ===== END V26__workflow_remove_permissions_conditions.sql =====

-- ===== BEGIN V27__workflow_definition_context.sql =====
ALTER TABLE workflow_definition
  ADD COLUMN IF NOT EXISTS context_type VARCHAR(60),
  ADD COLUMN IF NOT EXISTS context_id BIGINT;

ALTER TABLE workflow_definition
  DROP CONSTRAINT IF EXISTS ck_workflow_definition_context_type;

ALTER TABLE workflow_definition
  ADD CONSTRAINT ck_workflow_definition_context_type
  CHECK (context_type IS NULL OR context_type IN ('MOVIMENTO_CONFIG'));

ALTER TABLE workflow_definition
  DROP CONSTRAINT IF EXISTS ck_workflow_definition_context_pair;

ALTER TABLE workflow_definition
  ADD CONSTRAINT ck_workflow_definition_context_pair
  CHECK (
    (context_type IS NULL AND context_id IS NULL)
    OR (context_type IS NOT NULL AND context_id IS NOT NULL AND context_id > 0)
  );

DROP INDEX IF EXISTS ux_workflow_def_tenant_origin_version;
DROP INDEX IF EXISTS ux_workflow_def_tenant_origin_published;
DROP INDEX IF EXISTS idx_workflow_def_tenant_origin_status;

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_def_tenant_origin_ctx_version
  ON workflow_definition (
    tenant_id,
    origin,
    COALESCE(context_type, ''),
    COALESCE(context_id, -1),
    version_num
  );

CREATE UNIQUE INDEX IF NOT EXISTS ux_workflow_def_tenant_origin_ctx_published
  ON workflow_definition (
    tenant_id,
    origin,
    COALESCE(context_type, ''),
    COALESCE(context_id, -1)
  )
  WHERE status = 'PUBLISHED' AND active = TRUE;

CREATE INDEX IF NOT EXISTS idx_workflow_def_tenant_origin_ctx_status
  ON workflow_definition (
    tenant_id,
    origin,
    COALESCE(context_type, ''),
    COALESCE(context_id, -1),
    status,
    updated_at DESC
  );
-- ===== END V27__workflow_definition_context.sql =====

-- ===== BEGIN V28__catalog_search_indexes.sql =====
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_catalog_product_search_text_trgm
  ON catalog_product
  USING gin (lower(coalesce(nome, '') || ' ' || coalesce(descricao, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_search_text_trgm
  ON catalog_service_item
  USING gin (lower(coalesce(nome, '') || ' ' || coalesce(descricao, '')) gin_trgm_ops);
-- ===== END V28__catalog_search_indexes.sql =====

-- ===== BEGIN V29__units_schema.sql =====
CREATE TABLE IF NOT EXISTS official_unit (
  id UUID PRIMARY KEY,
  codigo_oficial VARCHAR(20) NOT NULL,
  descricao VARCHAR(160) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  origem VARCHAR(60) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_official_unit_codigo
  ON official_unit (upper(codigo_oficial));

CREATE INDEX IF NOT EXISTS idx_official_unit_ativo
  ON official_unit (ativo);

CREATE TABLE IF NOT EXISTS tenant_unit (
  id UUID PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  unidade_oficial_id UUID NOT NULL,
  sigla VARCHAR(20) NOT NULL,
  nome VARCHAR(160) NOT NULL,
  fator_para_oficial NUMERIC(24,12) NOT NULL,
  system_mirror BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_tenant_unit_fator_para_oficial_non_negative
    CHECK (fator_para_oficial >= 0),
  CONSTRAINT fk_tenant_unit_official_unit
    FOREIGN KEY (unidade_oficial_id)
    REFERENCES official_unit (id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_id_tenant
  ON tenant_unit (id, tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_tenant_sigla
  ON tenant_unit (tenant_id, lower(sigla));

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_tenant_oficial_sigla
  ON tenant_unit (tenant_id, unidade_oficial_id, lower(sigla));

CREATE INDEX IF NOT EXISTS idx_tenant_unit_tenant_oficial
  ON tenant_unit (tenant_id, unidade_oficial_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_tenant_mirror
  ON tenant_unit (tenant_id, system_mirror);

CREATE TABLE IF NOT EXISTS tenant_unit_conversion (
  id UUID PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  unidade_origem_id UUID NOT NULL,
  unidade_destino_id UUID NOT NULL,
  fator NUMERIC(24,12) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_tenant_unit_conversion_fator_positive
    CHECK (fator > 0),
  CONSTRAINT ck_tenant_unit_conversion_origem_destino_diff
    CHECK (unidade_origem_id <> unidade_destino_id),
  CONSTRAINT fk_tenant_unit_conversion_origem_tenant
    FOREIGN KEY (unidade_origem_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_tenant_unit_conversion_destino_tenant
    FOREIGN KEY (unidade_destino_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_unit_conversion_scope
  ON tenant_unit_conversion (tenant_id, unidade_origem_id, unidade_destino_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_conversion_tenant_origem
  ON tenant_unit_conversion (tenant_id, unidade_origem_id);

CREATE INDEX IF NOT EXISTS idx_tenant_unit_conversion_tenant_destino
  ON tenant_unit_conversion (tenant_id, unidade_destino_id);
-- ===== END V29__units_schema.sql =====

-- ===== BEGIN V30__catalog_units_lock.sql =====
ALTER TABLE catalog_product
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_alternativa_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS fator_conversao_alternativa NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS has_stock_movements BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE catalog_service_item
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_alternativa_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS fator_conversao_alternativa NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS has_stock_movements BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE catalog_product
  ADD CONSTRAINT ck_catalog_product_fator_alt_positive
    CHECK (fator_conversao_alternativa IS NULL OR fator_conversao_alternativa > 0);

ALTER TABLE catalog_service_item
  ADD CONSTRAINT ck_catalog_service_item_fator_alt_positive
    CHECK (fator_conversao_alternativa IS NULL OR fator_conversao_alternativa > 0);

ALTER TABLE catalog_product
  ADD CONSTRAINT ck_catalog_product_alt_unit_diff
    CHECK (
      unidade_alternativa_tenant_unit_id IS NULL
      OR tenant_unit_id IS NULL
      OR unidade_alternativa_tenant_unit_id <> tenant_unit_id
    );

ALTER TABLE catalog_service_item
  ADD CONSTRAINT ck_catalog_service_item_alt_unit_diff
    CHECK (
      unidade_alternativa_tenant_unit_id IS NULL
      OR tenant_unit_id IS NULL
      OR unidade_alternativa_tenant_unit_id <> tenant_unit_id
    );

ALTER TABLE catalog_product
  ADD CONSTRAINT fk_catalog_product_tenant_unit_base
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_product
  ADD CONSTRAINT fk_catalog_product_tenant_unit_alt
    FOREIGN KEY (unidade_alternativa_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_service_item
  ADD CONSTRAINT fk_catalog_service_item_tenant_unit_base
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_service_item
  ADD CONSTRAINT fk_catalog_service_item_tenant_unit_alt
    FOREIGN KEY (unidade_alternativa_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_catalog_product_tenant_unit
  ON catalog_product (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_product_alt_tenant_unit
  ON catalog_product (tenant_id, unidade_alternativa_tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_product_has_stock_movements
  ON catalog_product (tenant_id, has_stock_movements);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_tenant_unit
  ON catalog_service_item (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_alt_tenant_unit
  ON catalog_service_item (tenant_id, unidade_alternativa_tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_has_stock_movements
  ON catalog_service_item (tenant_id, has_stock_movements);
-- ===== END V30__catalog_units_lock.sql =====

-- ===== BEGIN V31__movement_units_history.sql =====
ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_base_catalogo_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS quantidade_convertida_base NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS fator_aplicado NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS fator_fonte VARCHAR(40);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_qtd_convertida_non_negative
    CHECK (quantidade_convertida_base IS NULL OR quantidade_convertida_base >= 0);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_fator_positive
    CHECK (fator_aplicado IS NULL OR fator_aplicado > 0);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_unit_informed
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_unit_base_catalog
    FOREIGN KEY (unidade_base_catalogo_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_tenant_unit
  ON movimento_estoque_item (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_base_unit
  ON movimento_estoque_item (tenant_id, unidade_base_catalogo_tenant_unit_id);

ALTER TABLE catalog_movement
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_base_catalogo_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS quantidade_informada NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS quantidade_convertida_base NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS fator_aplicado NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS fator_fonte VARCHAR(40);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_qtd_informada_non_negative
    CHECK (quantidade_informada IS NULL OR quantidade_informada >= 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_qtd_convertida_non_negative
    CHECK (quantidade_convertida_base IS NULL OR quantidade_convertida_base >= 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_fator_positive
    CHECK (fator_aplicado IS NULL OR fator_aplicado > 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT fk_catalog_movement_unit_informed
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_movement
  ADD CONSTRAINT fk_catalog_movement_unit_base_catalog
    FOREIGN KEY (unidade_base_catalogo_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_catalog_movement_tenant_unit
  ON catalog_movement (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_base_unit
  ON catalog_movement (tenant_id, unidade_base_catalogo_tenant_unit_id);
-- ===== END V31__movement_units_history.sql =====

-- ===== BEGIN V32__seed_un_for_existing_catalog_items.sql =====
-- Backfill de unidade base "UN" para itens de catalogo legados (produtos e servicos).
-- Regras:
-- 1) Garante a unidade oficial "UN".
-- 2) Para tenants com itens sem unidade base, garante uma tenant_unit vinculada a "UN".
-- 3) Preenche tenant_unit_id nulo em catalog_product e catalog_service_item.

WITH ensure_official_un AS (
  INSERT INTO official_unit (
    id,
    codigo_oficial,
    descricao,
    ativo,
    origem,
    created_at,
    created_by,
    updated_at,
    updated_by
  )
  SELECT
    (
      substr(md5('official_unit:UN'), 1, 8) || '-' ||
      substr(md5('official_unit:UN'), 9, 4) || '-' ||
      substr(md5('official_unit:UN'), 13, 4) || '-' ||
      substr(md5('official_unit:UN'), 17, 4) || '-' ||
      substr(md5('official_unit:UN'), 21, 12)
    )::uuid,
    'UN',
    'UNIDADE',
    TRUE,
    'NFE_TABELA_UNIDADE_COMERCIAL',
    NOW(),
    'flyway',
    NOW(),
    'flyway'
  WHERE NOT EXISTS (
    SELECT 1
    FROM official_unit
    WHERE upper(codigo_oficial) = 'UN'
  )
),
official_un AS (
  SELECT id, descricao
  FROM official_unit
  WHERE upper(codigo_oficial) = 'UN'
  ORDER BY created_at ASC, id ASC
  LIMIT 1
),
target_tenants AS (
  SELECT DISTINCT tenant_id
  FROM catalog_product
  WHERE tenant_unit_id IS NULL
  UNION
  SELECT DISTINCT tenant_id
  FROM catalog_service_item
  WHERE tenant_unit_id IS NULL
),
missing_tenant_un AS (
  SELECT t.tenant_id
  FROM target_tenants t
  CROSS JOIN official_un ou
  WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_unit tu
    WHERE tu.tenant_id = t.tenant_id
      AND tu.unidade_oficial_id = ou.id
  )
)
INSERT INTO tenant_unit (
  id,
  tenant_id,
  unidade_oficial_id,
  sigla,
  nome,
  fator_para_oficial,
  system_mirror,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  (
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 1, 8) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 9, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 13, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 17, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 21, 12)
  )::uuid,
  m.tenant_id,
  ou.id,
  CASE
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'un'
    ) THEN 'UN'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'unof'
    ) THEN 'UNOF'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = lower(left('UNOF' || m.tenant_id::text, 20))
    ) THEN left('UNOF' || m.tenant_id::text, 20)
    ELSE left('UN' || substr(md5('sigla:tenant:' || m.tenant_id::text || ':official:UN'), 1, 18), 20)
  END,
  COALESCE(NULLIF(trim(ou.descricao), ''), 'UNIDADE'),
  1.000000000000,
  TRUE,
  NOW(),
  'flyway',
  NOW(),
  'flyway'
FROM missing_tenant_un m
CROSS JOIN official_un ou;

UPDATE catalog_product cp
SET tenant_unit_id = tu.id
FROM (
  SELECT
    tu.tenant_id,
    tu.id,
    row_number() OVER (
      PARTITION BY tu.tenant_id
      ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
    ) AS rn
  FROM tenant_unit tu
  JOIN official_unit ou
    ON ou.id = tu.unidade_oficial_id
   AND upper(ou.codigo_oficial) = 'UN'
) tu
WHERE cp.tenant_unit_id IS NULL
  AND cp.tenant_id = tu.tenant_id
  AND tu.rn = 1;

UPDATE catalog_service_item cs
SET tenant_unit_id = tu.id
FROM (
  SELECT
    tu.tenant_id,
    tu.id,
    row_number() OVER (
      PARTITION BY tu.tenant_id
      ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
    ) AS rn
  FROM tenant_unit tu
  JOIN official_unit ou
    ON ou.id = tu.unidade_oficial_id
   AND upper(ou.codigo_oficial) = 'UN'
) tu
WHERE cs.tenant_unit_id IS NULL
  AND cs.tenant_id = tu.tenant_id
  AND tu.rn = 1;
-- ===== END V32__seed_un_for_existing_catalog_items.sql =====

-- ===== BEGIN V33__seed_un_for_existing_movement_items.sql =====
-- Backfill de unidade "UN" para itens de movimentos legados.
-- Complementa V32 ao preencher unidade em:
-- - movimento_estoque_item (tenant_unit_id e unidade_base_catalogo_tenant_unit_id)
-- - catalog_movement (tenant_unit_id e unidade_base_catalogo_tenant_unit_id)

WITH ensure_official_un AS (
  INSERT INTO official_unit (
    id,
    codigo_oficial,
    descricao,
    ativo,
    origem,
    created_at,
    created_by,
    updated_at,
    updated_by
  )
  SELECT
    (
      substr(md5('official_unit:UN'), 1, 8) || '-' ||
      substr(md5('official_unit:UN'), 9, 4) || '-' ||
      substr(md5('official_unit:UN'), 13, 4) || '-' ||
      substr(md5('official_unit:UN'), 17, 4) || '-' ||
      substr(md5('official_unit:UN'), 21, 12)
    )::uuid,
    'UN',
    'UNIDADE',
    TRUE,
    'NFE_TABELA_UNIDADE_COMERCIAL',
    NOW(),
    'flyway',
    NOW(),
    'flyway'
  WHERE NOT EXISTS (
    SELECT 1
    FROM official_unit
    WHERE upper(codigo_oficial) = 'UN'
  )
),
official_un AS (
  SELECT id, descricao
  FROM official_unit
  WHERE upper(codigo_oficial) = 'UN'
  ORDER BY created_at ASC, id ASC
  LIMIT 1
),
target_tenants AS (
  SELECT DISTINCT tenant_id
  FROM movimento_estoque_item
  WHERE tenant_unit_id IS NULL
     OR unidade_base_catalogo_tenant_unit_id IS NULL
  UNION
  SELECT DISTINCT tenant_id
  FROM catalog_movement
  WHERE tenant_unit_id IS NULL
     OR unidade_base_catalogo_tenant_unit_id IS NULL
),
missing_tenant_un AS (
  SELECT t.tenant_id
  FROM target_tenants t
  CROSS JOIN official_un ou
  WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_unit tu
    WHERE tu.tenant_id = t.tenant_id
      AND tu.unidade_oficial_id = ou.id
  )
)
INSERT INTO tenant_unit (
  id,
  tenant_id,
  unidade_oficial_id,
  sigla,
  nome,
  fator_para_oficial,
  system_mirror,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  (
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 1, 8) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 9, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 13, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 17, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 21, 12)
  )::uuid,
  m.tenant_id,
  ou.id,
  CASE
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'un'
    ) THEN 'UN'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'unof'
    ) THEN 'UNOF'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = lower(left('UNOF' || m.tenant_id::text, 20))
    ) THEN left('UNOF' || m.tenant_id::text, 20)
    ELSE left('UN' || substr(md5('sigla:tenant:' || m.tenant_id::text || ':official:UN'), 1, 18), 20)
  END,
  COALESCE(NULLIF(trim(ou.descricao), ''), 'UNIDADE'),
  1.000000000000,
  TRUE,
  NOW(),
  'flyway',
  NOW(),
  'flyway'
FROM missing_tenant_un m
CROSS JOIN official_un ou;

WITH tenant_un AS (
  SELECT tenant_id, id
  FROM (
    SELECT
      tu.tenant_id,
      tu.id,
      row_number() OVER (
        PARTITION BY tu.tenant_id
        ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
      ) AS rn
    FROM tenant_unit tu
    JOIN official_unit ou
      ON ou.id = tu.unidade_oficial_id
     AND upper(ou.codigo_oficial) = 'UN'
  ) ranked
  WHERE rn = 1
)
UPDATE movimento_estoque_item mei
SET
  unidade_base_catalogo_tenant_unit_id = COALESCE(
    mei.unidade_base_catalogo_tenant_unit_id,
    CASE
      WHEN mei.catalog_type = 'PRODUCTS' THEN (
        SELECT cp.tenant_unit_id
        FROM catalog_product cp
        WHERE cp.tenant_id = mei.tenant_id
          AND cp.id = mei.catalog_item_id
        LIMIT 1
      )
      WHEN mei.catalog_type = 'SERVICES' THEN (
        SELECT cs.tenant_unit_id
        FROM catalog_service_item cs
        WHERE cs.tenant_id = mei.tenant_id
          AND cs.id = mei.catalog_item_id
        LIMIT 1
      )
      ELSE NULL
    END,
    tu.id
  ),
  tenant_unit_id = COALESCE(
    mei.tenant_unit_id,
    mei.unidade_base_catalogo_tenant_unit_id,
    CASE
      WHEN mei.catalog_type = 'PRODUCTS' THEN (
        SELECT cp.tenant_unit_id
        FROM catalog_product cp
        WHERE cp.tenant_id = mei.tenant_id
          AND cp.id = mei.catalog_item_id
        LIMIT 1
      )
      WHEN mei.catalog_type = 'SERVICES' THEN (
        SELECT cs.tenant_unit_id
        FROM catalog_service_item cs
        WHERE cs.tenant_id = mei.tenant_id
          AND cs.id = mei.catalog_item_id
        LIMIT 1
      )
      ELSE NULL
    END,
    tu.id
  )
FROM tenant_un tu
WHERE mei.tenant_id = tu.tenant_id
  AND (mei.tenant_unit_id IS NULL OR mei.unidade_base_catalogo_tenant_unit_id IS NULL);

WITH tenant_un AS (
  SELECT tenant_id, id
  FROM (
    SELECT
      tu.tenant_id,
      tu.id,
      row_number() OVER (
        PARTITION BY tu.tenant_id
        ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
      ) AS rn
    FROM tenant_unit tu
    JOIN official_unit ou
      ON ou.id = tu.unidade_oficial_id
     AND upper(ou.codigo_oficial) = 'UN'
  ) ranked
  WHERE rn = 1
)
UPDATE catalog_movement cm
SET
  unidade_base_catalogo_tenant_unit_id = COALESCE(
    cm.unidade_base_catalogo_tenant_unit_id,
    CASE
      WHEN cm.catalog_type = 'PRODUCTS' THEN (
        SELECT cp.tenant_unit_id
        FROM catalog_product cp
        WHERE cp.tenant_id = cm.tenant_id
          AND cp.id = cm.catalogo_id
        LIMIT 1
      )
      WHEN cm.catalog_type = 'SERVICES' THEN (
        SELECT cs.tenant_unit_id
        FROM catalog_service_item cs
        WHERE cs.tenant_id = cm.tenant_id
          AND cs.id = cm.catalogo_id
        LIMIT 1
      )
      ELSE NULL
    END,
    tu.id
  ),
  tenant_unit_id = COALESCE(
    cm.tenant_unit_id,
    cm.unidade_base_catalogo_tenant_unit_id,
    CASE
      WHEN cm.catalog_type = 'PRODUCTS' THEN (
        SELECT cp.tenant_unit_id
        FROM catalog_product cp
        WHERE cp.tenant_id = cm.tenant_id
          AND cp.id = cm.catalogo_id
        LIMIT 1
      )
      WHEN cm.catalog_type = 'SERVICES' THEN (
        SELECT cs.tenant_unit_id
        FROM catalog_service_item cs
        WHERE cs.tenant_id = cm.tenant_id
          AND cs.id = cm.catalogo_id
        LIMIT 1
      )
      ELSE NULL
    END,
    tu.id
  )
FROM tenant_un tu
WHERE cm.tenant_id = tu.tenant_id
  AND (cm.tenant_unit_id IS NULL OR cm.unidade_base_catalogo_tenant_unit_id IS NULL);
-- ===== END V33__seed_un_for_existing_movement_items.sql =====

-- ===== BEGIN V34__movement_codes.sql =====
-- Codigo sequencial por configuracao para movimento_estoque
-- e codigo sequencial por movimento para movimento_estoque_item.

ALTER TABLE movimento_estoque
  ADD COLUMN IF NOT EXISTS codigo BIGINT;

ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS codigo BIGINT;

WITH ranked_mov AS (
  SELECT
    me.id,
    row_number() OVER (
      PARTITION BY me.tenant_id, me.movimento_config_id
      ORDER BY me.created_at ASC, me.id ASC
    ) AS next_codigo
  FROM movimento_estoque me
  WHERE me.codigo IS NULL
)
UPDATE movimento_estoque me
SET codigo = ranked_mov.next_codigo
FROM ranked_mov
WHERE me.id = ranked_mov.id;

WITH ranked_item AS (
  SELECT
    mei.id,
    row_number() OVER (
      PARTITION BY mei.tenant_id, mei.movimento_estoque_id
      ORDER BY mei.ordem ASC, mei.id ASC
    ) AS next_codigo
  FROM movimento_estoque_item mei
  WHERE mei.codigo IS NULL
)
UPDATE movimento_estoque_item mei
SET codigo = ranked_item.next_codigo
FROM ranked_item
WHERE mei.id = ranked_item.id;

CREATE TABLE IF NOT EXISTS movimento_estoque_codigo_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_mov_estoque_codigo_seq_config_tenant
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_codigo_seq_scope
  ON movimento_estoque_codigo_seq (tenant_id, movimento_config_id);

INSERT INTO movimento_estoque_codigo_seq (
  tenant_id,
  movimento_config_id,
  next_value,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  me.tenant_id,
  me.movimento_config_id,
  COALESCE(MAX(me.codigo), 0) + 1,
  NOW(),
  'flyway',
  NOW(),
  'flyway'
FROM movimento_estoque me
GROUP BY me.tenant_id, me.movimento_config_id
ON CONFLICT (tenant_id, movimento_config_id)
DO UPDATE SET
  next_value = GREATEST(movimento_estoque_codigo_seq.next_value, EXCLUDED.next_value),
  updated_at = NOW(),
  updated_by = 'flyway';

ALTER TABLE movimento_estoque
  ALTER COLUMN codigo SET NOT NULL;

ALTER TABLE movimento_estoque_item
  ALTER COLUMN codigo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_codigo_scope
  ON movimento_estoque (tenant_id, movimento_config_id, codigo);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_item_codigo_scope
  ON movimento_estoque_item (tenant_id, movimento_estoque_id, codigo);
-- ===== END V34__movement_codes.sql =====

-- ===== BEGIN V35__permissao_movimento_estoque_desfazer.sql =====
-- ===== BEGIN V35__permissao_movimento_estoque_desfazer.sql =====

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES (1, 'MOVIMENTO_ESTOQUE_DESFAZER', 'Desfazer movimentacao de estoque do item', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, 'MOVIMENTO_ESTOQUE_DESFAZER', NOW(), NOW()
FROM papel p
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = 'MOVIMENTO_ESTOQUE_DESFAZER'
  );

-- ===== END V35__permissao_movimento_estoque_desfazer.sql =====
-- ===== END V35__permissao_movimento_estoque_desfazer.sql =====

-- ===== BEGIN V36__catalog_prices.sql =====
-- ===== BEGIN V36__catalog_prices.sql =====

CREATE TABLE IF NOT EXISTS catalog_price_rule_by_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_by_group_id BIGINT NOT NULL,
  price_type VARCHAR(20) NOT NULL,
  custom_name VARCHAR(80),
  base_mode VARCHAR(20) NOT NULL,
  base_price_type VARCHAR(20),
  adjustment_kind_default VARCHAR(20) NOT NULL,
  adjustment_default NUMERIC(19,6) NOT NULL DEFAULT 0,
  ui_lock_mode VARCHAR(10) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_price_rule_type CHECK (price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE')),
  CONSTRAINT ck_catalog_price_rule_base_mode CHECK (base_mode IN ('NONE', 'BASE_PRICE')),
  CONSTRAINT ck_catalog_price_rule_base_type CHECK (
    (base_mode = 'NONE' AND base_price_type IS NULL)
    OR
    (base_mode = 'BASE_PRICE' AND base_price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE'))
  ),
  CONSTRAINT ck_catalog_price_rule_adjust_kind CHECK (adjustment_kind_default IN ('FIXED', 'PERCENT')),
  CONSTRAINT ck_catalog_price_rule_lock_mode CHECK (ui_lock_mode IN ('I', 'II', 'III', 'IV')),
  CONSTRAINT fk_catalog_price_rule_group_cfg
    FOREIGN KEY (catalog_configuration_by_group_id)
    REFERENCES catalog_configuration_by_group (id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_price_rule_group_type
  ON catalog_price_rule_by_group (tenant_id, catalog_configuration_by_group_id, price_type);

CREATE INDEX IF NOT EXISTS idx_catalog_price_rule_group
  ON catalog_price_rule_by_group (tenant_id, catalog_configuration_by_group_id, active);

CREATE TABLE IF NOT EXISTS catalog_item_price (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  price_type VARCHAR(20) NOT NULL,
  price_final NUMERIC(19,6) NOT NULL DEFAULT 0,
  adjustment_kind VARCHAR(20) NOT NULL,
  adjustment_value NUMERIC(19,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_item_price_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_catalog_item_price_type CHECK (price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE')),
  CONSTRAINT ck_catalog_item_price_adjust_kind CHECK (adjustment_kind IN ('FIXED', 'PERCENT')),
  CONSTRAINT ck_catalog_item_price_non_negative CHECK (price_final >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_item_price_scope
  ON catalog_item_price (tenant_id, catalog_type, catalog_item_id, price_type);

CREATE INDEX IF NOT EXISTS idx_catalog_item_price_item
  ON catalog_item_price (tenant_id, catalog_type, catalog_item_id);

CREATE TABLE IF NOT EXISTS price_book (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_tenant_name
  ON price_book (tenant_id, lower(name));

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_tenant_default
  ON price_book (tenant_id)
  WHERE is_default = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_id_tenant
  ON price_book (id, tenant_id);

CREATE TABLE IF NOT EXISTS price_variant (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_variant_tenant_name
  ON price_variant (tenant_id, lower(name));

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_variant_id_tenant
  ON price_variant (id, tenant_id);

CREATE TABLE IF NOT EXISTS sale_price (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  price_book_id BIGINT NOT NULL,
  variant_id BIGINT,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  tenant_unit_id UUID,
  price_final NUMERIC(19,6) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_sale_price_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_sale_price_non_negative CHECK (price_final >= 0),
  CONSTRAINT fk_sale_price_book_tenant
    FOREIGN KEY (price_book_id, tenant_id)
    REFERENCES price_book (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_sale_price_variant_tenant
    FOREIGN KEY (variant_id, tenant_id)
    REFERENCES price_variant (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_sale_price_tenant_unit
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sale_price_scope
  ON sale_price (
    tenant_id,
    price_book_id,
    COALESCE(variant_id, 0),
    catalog_type,
    catalog_item_id,
    COALESCE(tenant_unit_id, '00000000-0000-0000-0000-000000000000'::UUID)
  );

CREATE INDEX IF NOT EXISTS idx_sale_price_lookup
  ON sale_price (tenant_id, price_book_id, variant_id, catalog_type, catalog_item_id, tenant_unit_id);

CREATE TABLE IF NOT EXISTS price_change_log (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sale_price_id BIGINT,
  action VARCHAR(20) NOT NULL,
  old_price_final NUMERIC(19,6),
  new_price_final NUMERIC(19,6),
  price_book_id BIGINT,
  variant_id BIGINT,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  tenant_unit_id UUID,
  changed_by VARCHAR(120),
  changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_price_change_log_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
  CONSTRAINT ck_price_change_log_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES'))
);

CREATE INDEX IF NOT EXISTS idx_price_change_log_scope
  ON price_change_log (tenant_id, price_book_id, variant_id, catalog_type, catalog_item_id, changed_at DESC);

ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS unit_price_applied NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS price_book_id_snapshot BIGINT,
  ADD COLUMN IF NOT EXISTS variant_id_snapshot BIGINT,
  ADD COLUMN IF NOT EXISTS sale_price_source_snapshot VARCHAR(40),
  ADD COLUMN IF NOT EXISTS sale_price_id_snapshot BIGINT;

UPDATE movimento_estoque_item
SET unit_price_applied = COALESCE(valor_unitario, 0)
WHERE unit_price_applied IS NULL;

ALTER TABLE movimento_estoque_item
  ALTER COLUMN unit_price_applied SET NOT NULL;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_unit_price_applied_non_negative
    CHECK (unit_price_applied >= 0);

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_price_book_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_price_book_snapshot
  FOREIGN KEY (price_book_id_snapshot, tenant_id)
  REFERENCES price_book (id, tenant_id)
  ON DELETE SET NULL;

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_variant_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_variant_snapshot
  FOREIGN KEY (variant_id_snapshot, tenant_id)
  REFERENCES price_variant (id, tenant_id)
  ON DELETE SET NULL;

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_sale_price_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_sale_price_snapshot
  FOREIGN KEY (sale_price_id_snapshot)
  REFERENCES sale_price (id)
  ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_price_snapshot
  ON movimento_estoque_item (tenant_id, price_book_id_snapshot, variant_id_snapshot, catalog_type, catalog_item_id);

ALTER TABLE registro_entidade
  ADD COLUMN IF NOT EXISTS price_book_id BIGINT;

ALTER TABLE registro_entidade
  DROP CONSTRAINT IF EXISTS fk_registro_entidade_price_book;

ALTER TABLE registro_entidade
  ADD CONSTRAINT fk_registro_entidade_price_book
  FOREIGN KEY (price_book_id, tenant_id)
  REFERENCES price_book (id, tenant_id)
  ON DELETE SET NULL;

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES
  (1, 'CATALOG_PRICES_VIEW', 'Visualizar precos de catalogo', TRUE, NOW(), NOW()),
  (1, 'CATALOG_PRICES_MANAGE', 'Gerenciar precos de catalogo', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, pc.codigo, NOW(), NOW()
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND pc.codigo IN ('CATALOG_PRICES_VIEW', 'CATALOG_PRICES_MANAGE')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = pc.codigo
  );

-- ===== END V36__catalog_prices.sql =====
-- ===== END V36__catalog_prices.sql =====

-- ===== BEGIN V37__stock_origin_and_price_history_enhancements.sql =====

ALTER TABLE catalog_movement
  ADD COLUMN IF NOT EXISTS origem_movimentacao_id BIGINT,
  ADD COLUMN IF NOT EXISTS movimento_tipo VARCHAR(40),
  ADD COLUMN IF NOT EXISTS workflow_origin VARCHAR(60),
  ADD COLUMN IF NOT EXISTS workflow_entity_id BIGINT,
  ADD COLUMN IF NOT EXISTS workflow_transition_key VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_origem_id
  ON catalog_movement (tenant_id, origem_movimentacao_id);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_tipo_movimento
  ON catalog_movement (tenant_id, movimento_tipo);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_usuario
  ON catalog_movement (tenant_id, created_by);

ALTER TABLE price_change_log
  ADD COLUMN IF NOT EXISTS source_type VARCHAR(40),
  ADD COLUMN IF NOT EXISTS origin_type VARCHAR(40),
  ADD COLUMN IF NOT EXISTS origin_id BIGINT,
  ADD COLUMN IF NOT EXISTS price_type VARCHAR(20),
  ADD COLUMN IF NOT EXISTS price_book_name VARCHAR(120);

ALTER TABLE price_change_log
  DROP CONSTRAINT IF EXISTS ck_price_change_log_source_type;

ALTER TABLE price_change_log
  ADD CONSTRAINT ck_price_change_log_source_type
    CHECK (
      source_type IS NULL
      OR source_type IN ('SALE_PRICE', 'CATALOG_ITEM_PRICE')
    );

ALTER TABLE price_change_log
  DROP CONSTRAINT IF EXISTS ck_price_change_log_origin_type;

ALTER TABLE price_change_log
  ADD CONSTRAINT ck_price_change_log_origin_type
    CHECK (
      origin_type IS NULL
      OR origin_type IN ('ALTERACAO_TABELA_PRECO', 'ALTERACAO_PRECO_BASE')
    );

ALTER TABLE price_change_log
  DROP CONSTRAINT IF EXISTS ck_price_change_log_price_type;

ALTER TABLE price_change_log
  ADD CONSTRAINT ck_price_change_log_price_type
    CHECK (
      price_type IS NULL
      OR price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE')
    );

CREATE INDEX IF NOT EXISTS idx_price_change_log_item_filters
  ON price_change_log (
    tenant_id,
    catalog_type,
    catalog_item_id,
    source_type,
    price_book_id,
    price_type,
    changed_at DESC
  );

CREATE INDEX IF NOT EXISTS idx_price_change_log_origin
  ON price_change_log (tenant_id, origin_type, origin_id, changed_at DESC);

-- ===== END V37__stock_origin_and_price_history_enhancements.sql =====

