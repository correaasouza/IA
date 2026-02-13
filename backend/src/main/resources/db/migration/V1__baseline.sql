<<<<<<< HEAD
﻿-- V1__create_locatario.sql
=======
-- baseline consolidado apos reset de migracoes
-- inclui schema base de seguranca/config e schema de entidades/pessoas/relatorios

>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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

<<<<<<< HEAD

-- V2__seed_master_locatario.sql
INSERT INTO locatario (id, nome, data_limite_acesso, ativo, created_at, updated_at)
VALUES (1, 'Master', '2099-12-31', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;


-- V3__create_usuario.sql
=======
INSERT INTO locatario (id, nome, data_limite_acesso, ativo, created_at, updated_at)
VALUES (1, 'Master', '2099-12-31', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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
<<<<<<< HEAD


-- V4__fase4_base.sql
CREATE TABLE tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_tipo_entidade_tenant ON tipo_entidade (tenant_id);

CREATE TABLE campo_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  nome VARCHAR(120) NOT NULL,
  label VARCHAR(120),
  tipo VARCHAR(40) NOT NULL,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  tamanho INTEGER,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_campo_definicao_tenant ON campo_definicao (tenant_id);
CREATE INDEX idx_campo_definicao_tipo ON campo_definicao (tipo_entidade_id);

CREATE TABLE registro_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_registro_entidade_tenant ON registro_entidade (tenant_id);
CREATE INDEX idx_registro_entidade_tipo ON registro_entidade (tipo_entidade_id);

CREATE TABLE registro_campo_valor (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL REFERENCES registro_entidade(id),
  campo_definicao_id BIGINT NOT NULL REFERENCES campo_definicao(id),
  valor_texto TEXT,
  valor_numero NUMERIC(18,2),
  valor_data DATE,
  valor_booleano BOOLEAN,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_registro_campo_tenant ON registro_campo_valor (tenant_id);
=======
CREATE UNIQUE INDEX idx_usuario_tenant_username ON usuario (tenant_id, username);
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)

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

CREATE INDEX idx_config_coluna_tenant ON config_coluna (tenant_id);

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

CREATE INDEX idx_config_formulario_tenant ON config_formulario (tenant_id);

<<<<<<< HEAD

-- V5__entidades_fixas.sql
CREATE TABLE entidade_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_entidade_definicao_unique ON entidade_definicao (tenant_id, codigo);
CREATE INDEX idx_entidade_definicao_tenant ON entidade_definicao (tenant_id);

CREATE TABLE entidade_registro (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL REFERENCES entidade_definicao(id),
  nome VARCHAR(200) NOT NULL,
  apelido VARCHAR(200),
  cpf_cnpj VARCHAR(20) NOT NULL,
  tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_entidade_registro_tenant ON entidade_registro (tenant_id);
CREATE INDEX idx_entidade_registro_definicao ON entidade_registro (entidade_definicao_id);


-- V6__seed_entidades_fixas.sql
INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'CLIENTE', 'Cliente', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'FUNCIONARIO', 'Funcionário', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'FORNECEDOR', 'Fornecedor', true, NOW(), NOW())
ON CONFLICT DO NOTHING;


-- V7__entidade_registro_ativo.sql
ALTER TABLE entidade_registro
  ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;


-- V8__entidade_definicao_role.sql
ALTER TABLE entidade_definicao
  ADD COLUMN IF NOT EXISTS role_required VARCHAR(120);


-- V9__contato.sql
CREATE TABLE contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_registro_id BIGINT NOT NULL REFERENCES entidade_registro(id),
  tipo VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_contato_tenant ON contato (tenant_id);
CREATE INDEX idx_contato_entidade ON contato (entidade_registro_id);


-- V10__contato_tipo.sql
CREATE TABLE contato_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(30) NOT NULL,
  nome VARCHAR(80) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  mascara VARCHAR(60),
  regex_validacao VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_contato_tipo_unique ON contato_tipo (tenant_id, codigo);
CREATE INDEX idx_contato_tipo_tenant ON contato_tipo (tenant_id);


-- V11__seed_contato_tipo.sql
INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'TELEFONE', 'Telefone', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'WHATSAPP', 'WhatsApp', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'EMAIL', 'E-mail', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;


-- V12__contato_tipo_mask.sql
ALTER TABLE contato_tipo
  ADD COLUMN IF NOT EXISTS mascara VARCHAR(60);
ALTER TABLE contato_tipo
  ADD COLUMN IF NOT EXISTS regex_validacao VARCHAR(200);


-- V13__contato_tipo_entidade.sql
CREATE TABLE contato_tipo_por_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL REFERENCES entidade_definicao(id),
  contato_tipo_id BIGINT NOT NULL REFERENCES contato_tipo(id),
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX idx_contato_tipo_entidade_unique
  ON contato_tipo_por_entidade (tenant_id, entidade_definicao_id, contato_tipo_id);


-- V14__atalho_usuario.sql
=======
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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

CREATE UNIQUE INDEX idx_atalho_usuario_unique ON atalho_usuario (tenant_id, user_id, menu_id);
CREATE INDEX idx_atalho_usuario_tenant ON atalho_usuario (tenant_id, user_id);

<<<<<<< HEAD

-- V15__papeis.sql
=======
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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

CREATE UNIQUE INDEX ux_papel_permissao ON papel_permissao (papel_id, permissao_codigo);
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

CREATE UNIQUE INDEX ux_usuario_papel ON usuario_papel (tenant_id, usuario_id, papel_id);
CREATE INDEX idx_usuario_papel_tenant ON usuario_papel (tenant_id);
CREATE INDEX idx_usuario_papel_usuario ON usuario_papel (usuario_id);

<<<<<<< HEAD

-- V16__permissao_catalogo.sql
=======
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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

<<<<<<< HEAD

-- V17__seed_permissoes.sql
=======
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'MASTER_ADMIN', 'Master Admin', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'MASTER_ADMIN'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
<<<<<<< HEAD
SELECT l.id, 'TENANT_ADMIN', 'Admin do Locatário', TRUE
=======
SELECT l.id, 'TENANT_ADMIN', 'Admin do Locatario', TRUE
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'TENANT_ADMIN'
);

<<<<<<< HEAD

-- V18__auditoria_evento.sql
=======
INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'CONFIG_EDITOR', 'Configurar colunas e formularios', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'CONFIG_EDITOR'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'USUARIO_MANAGE', 'Gerenciar usuarios', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'USUARIO_MANAGE'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'PAPEL_MANAGE', 'Gerenciar papeis', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'PAPEL_MANAGE'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'ADMIN', 'Administrador do locatario', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'ADMIN'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'USUARIO', 'Usuario padrao', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'USUARIO'
);

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo)
SELECT p.tenant_id, p.id, pc.codigo
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.nome = 'ADMIN'
  AND pc.codigo IN ('CONFIG_EDITOR','USUARIO_MANAGE','PAPEL_MANAGE')
  AND NOT EXISTS (
    SELECT 1 FROM papel_permissao pp
    WHERE pp.papel_id = p.id AND pp.permissao_codigo = pc.codigo
  );

>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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


<<<<<<< HEAD
-- V19__cleanup_permissoes.sql
DELETE FROM permissao_catalogo
WHERE codigo NOT IN ('MASTER_ADMIN', 'TENANT_ADMIN');


-- V20__seed_permissoes_granulares.sql
INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'CONFIG_EDITOR', 'Configurar colunas e formulários', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'CONFIG_EDITOR'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'USUARIO_MANAGE', 'Gerenciar usuários', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'USUARIO_MANAGE'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'PAPEL_MANAGE', 'Gerenciar papéis', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'PAPEL_MANAGE'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'RELATORIO_VIEW', 'Visualizar relatórios', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'RELATORIO_VIEW'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'ENTIDADE_EDIT', 'Editar entidades', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'ENTIDADE_EDIT'
);


-- V21__seed_papeis_default.sql
INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'ADMIN', 'Administrador do locatário', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'ADMIN'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'USUARIO', 'Usuário padrão', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'USUARIO'
);

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo)
SELECT p.tenant_id, p.id, pc.codigo
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.nome = 'ADMIN'
  AND pc.codigo IN ('CONFIG_EDITOR','USUARIO_MANAGE','PAPEL_MANAGE','RELATORIO_VIEW','ENTIDADE_EDIT')
  AND NOT EXISTS (
    SELECT 1 FROM papel_permissao pp
    WHERE pp.papel_id = p.id AND pp.permissao_codigo = pc.codigo
  );


-- V22__pessoa_entidade_refactor.sql
ALTER TABLE tipo_entidade ADD COLUMN IF NOT EXISTS codigo VARCHAR(40);
ALTER TABLE tipo_entidade ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE tipo_entidade
SET codigo = UPPER(REGEXP_REPLACE(nome, '\s+', '_', 'g'))
WHERE codigo IS NULL;

WITH duplicates AS (
  SELECT tenant_id, codigo, MIN(id) AS keep_id
  FROM tipo_entidade
  GROUP BY tenant_id, codigo
  HAVING COUNT(*) > 1
)
DELETE FROM tipo_entidade t
USING duplicates d
WHERE t.tenant_id = d.tenant_id
  AND t.codigo = d.codigo
  AND t.id <> d.keep_id;

ALTER TABLE tipo_entidade ALTER COLUMN codigo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tipo_entidade_unique_codigo
  ON tipo_entidade (tenant_id, codigo);

CREATE TABLE pessoa (
=======
-- bloco consolidado anteriormente em V5
CREATE TABLE IF NOT EXISTS tipo_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_entidade_tenant_codigo
  ON tipo_entidade (tenant_id, codigo);
CREATE INDEX IF NOT EXISTS idx_tipo_entidade_tenant
  ON tipo_entidade (tenant_id);

CREATE TABLE IF NOT EXISTS campo_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  label VARCHAR(120),
  tipo VARCHAR(40) NOT NULL,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  tamanho INTEGER,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_campo_definicao_tenant_tipo
  ON campo_definicao (tenant_id, tipo_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_definicao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  role_required VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_entidade_definicao_tenant_codigo
  ON entidade_definicao (tenant_id, codigo);
CREATE INDEX IF NOT EXISTS idx_entidade_definicao_tenant
  ON entidade_definicao (tenant_id);

CREATE TABLE IF NOT EXISTS entidade_registro (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  apelido VARCHAR(200),
  cpf_cnpj VARCHAR(20) NOT NULL,
  tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_entidade_registro_tenant_definicao
  ON entidade_registro (tenant_id, entidade_definicao_id);
CREATE INDEX IF NOT EXISTS idx_entidade_registro_cpf_cnpj
  ON entidade_registro (tenant_id, entidade_definicao_id, cpf_cnpj);
CREATE INDEX IF NOT EXISTS idx_entidade_registro_nome
  ON entidade_registro (tenant_id, entidade_definicao_id, nome);

CREATE TABLE IF NOT EXISTS contato_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  codigo VARCHAR(30) NOT NULL,
  nome VARCHAR(80) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  mascara VARCHAR(60),
  regex_validacao VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_contato_tipo_tenant_codigo
  ON contato_tipo (tenant_id, codigo);
CREATE INDEX IF NOT EXISTS idx_contato_tipo_tenant
  ON contato_tipo (tenant_id);

CREATE TABLE IF NOT EXISTS contato_tipo_por_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_definicao_id BIGINT NOT NULL,
  contato_tipo_id BIGINT NOT NULL,
  obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
  principal_unico BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_contato_tipo_por_entidade
  ON contato_tipo_por_entidade (tenant_id, entidade_definicao_id, contato_tipo_id);
CREATE INDEX IF NOT EXISTS idx_contato_tipo_por_entidade_tenant
  ON contato_tipo_por_entidade (tenant_id);

CREATE TABLE IF NOT EXISTS contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entidade_registro_id BIGINT NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_contato_tenant_registro
  ON contato (tenant_id, entidade_registro_id);

CREATE TABLE IF NOT EXISTS registro_entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_registro_entidade_tenant_tipo
  ON registro_entidade (tenant_id, tipo_entidade_id);

CREATE TABLE IF NOT EXISTS registro_campo_valor (
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

CREATE INDEX IF NOT EXISTS idx_registro_campo_valor_tenant_registro
  ON registro_campo_valor (tenant_id, registro_entidade_id);
CREATE INDEX IF NOT EXISTS idx_registro_campo_valor_tenant_campo
  ON registro_campo_valor (tenant_id, campo_definicao_id);

CREATE TABLE IF NOT EXISTS pessoa (
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
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

<<<<<<< HEAD
CREATE INDEX idx_pessoa_tenant ON pessoa (tenant_id);
CREATE UNIQUE INDEX idx_pessoa_cpf_unique ON pessoa (tenant_id, cpf) WHERE cpf IS NOT NULL;
CREATE UNIQUE INDEX idx_pessoa_cnpj_unique ON pessoa (tenant_id, cnpj) WHERE cnpj IS NOT NULL;
CREATE UNIQUE INDEX idx_pessoa_estrangeiro_unique ON pessoa (tenant_id, id_estrangeiro) WHERE id_estrangeiro IS NOT NULL;

CREATE TABLE entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
  pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX idx_entidade_tenant ON entidade (tenant_id);
CREATE INDEX idx_entidade_tipo ON entidade (tipo_entidade_id);
CREATE INDEX idx_entidade_pessoa ON entidade (pessoa_id);

CREATE TABLE pessoa_contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL REFERENCES pessoa(id),
=======
CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_tenant_cpf
  ON pessoa (tenant_id, cpf);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_tenant_cnpj
  ON pessoa (tenant_id, cnpj);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pessoa_tenant_id_estrangeiro
  ON pessoa (tenant_id, id_estrangeiro);
CREATE INDEX IF NOT EXISTS idx_pessoa_tenant
  ON pessoa (tenant_id);

CREATE TABLE IF NOT EXISTS pessoa_contato (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL,
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
  tipo VARCHAR(30) NOT NULL,
  valor VARCHAR(200) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

<<<<<<< HEAD
CREATE INDEX idx_pessoa_contato_tenant ON pessoa_contato (tenant_id);
CREATE INDEX idx_pessoa_contato_pessoa ON pessoa_contato (pessoa_id);

CREATE TABLE tipo_entidade_campo_regra (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL REFERENCES tipo_entidade(id),
=======
CREATE INDEX IF NOT EXISTS idx_pessoa_contato_tenant_pessoa
  ON pessoa_contato (tenant_id, pessoa_id);

CREATE TABLE IF NOT EXISTS entidade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
  pessoa_id BIGINT NOT NULL,
  alerta VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_entidade_tenant
  ON entidade (tenant_id);
CREATE INDEX IF NOT EXISTS idx_entidade_tenant_tipo
  ON entidade (tenant_id, tipo_entidade_id);

CREATE TABLE IF NOT EXISTS tipo_entidade_campo_regra (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tipo_entidade_id BIGINT NOT NULL,
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
  campo VARCHAR(60) NOT NULL,
  habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  requerido BOOLEAN NOT NULL DEFAULT FALSE,
  visivel BOOLEAN NOT NULL DEFAULT TRUE,
  editavel BOOLEAN NOT NULL DEFAULT TRUE,
  label VARCHAR(120),
  versao INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

<<<<<<< HEAD
CREATE INDEX idx_tipo_entidade_campo_tenant ON tipo_entidade_campo_regra (tenant_id);
CREATE INDEX idx_tipo_entidade_campo_tipo ON tipo_entidade_campo_regra (tipo_entidade_id);


-- V23__seed_tipo_entidade_por_tenant.sql
INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'CLIENTE', 'Cliente', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'FORNECEDOR', 'Fornecedor', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'FUNCIONARIO', 'Funcionário', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;


-- V24__migrar_pessoa_entidade.sql
WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    er.entidade_definicao_id,
    er.nome,
    er.apelido,
    er.ativo,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO pessoa (tenant_id, nome, apelido, cpf, cnpj, id_estrangeiro, ativo, versao, created_at, updated_at)
SELECT DISTINCT
  tenant_id,
  nome,
  apelido,
  cpf,
  cnpj,
  id_estrangeiro,
  TRUE,
  1,
  NOW(),
  NOW()
FROM origem
ON CONFLICT DO NOTHING;

WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    er.entidade_definicao_id,
    er.nome,
    er.apelido,
    er.ativo,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO entidade (tenant_id, tipo_entidade_id, pessoa_id, ativo, versao, created_at, updated_at)
SELECT
  o.tenant_id,
  te.id,
  p.id,
  o.ativo,
  1,
  NOW(),
  NOW()
FROM origem o
JOIN entidade_definicao ed ON ed.id = o.entidade_definicao_id
JOIN tipo_entidade te ON te.tenant_id = o.tenant_id AND te.codigo = ed.codigo
JOIN pessoa p ON p.tenant_id = o.tenant_id AND (
  (o.cpf IS NOT NULL AND p.cpf = o.cpf) OR
  (o.cnpj IS NOT NULL AND p.cnpj = o.cnpj) OR
  (o.id_estrangeiro IS NOT NULL AND p.id_estrangeiro = o.id_estrangeiro)
);

WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO pessoa_contato (tenant_id, pessoa_id, tipo, valor, principal, created_at, updated_at)
SELECT
  c.tenant_id,
  p.id,
  c.tipo,
  c.valor,
  c.principal,
  c.created_at,
  c.updated_at
FROM contato c
JOIN entidade_registro er ON er.id = c.entidade_registro_id
JOIN origem o ON o.id = er.id
JOIN pessoa p ON p.tenant_id = o.tenant_id AND (
  (o.cpf IS NOT NULL AND p.cpf = o.cpf) OR
  (o.cnpj IS NOT NULL AND p.cnpj = o.cnpj) OR
  (o.id_estrangeiro IS NOT NULL AND p.id_estrangeiro = o.id_estrangeiro)
);


-- V25__entidade_campos_papel.sql
ALTER TABLE entidade
  ADD COLUMN codigo_externo VARCHAR(80),
  ADD COLUMN observacao TEXT;


-- V26__entidade_alerta.sql
ALTER TABLE entidade
  RENAME COLUMN observacao TO alerta;

ALTER TABLE entidade
  DROP COLUMN IF EXISTS codigo_externo;


=======
CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_entidade_campo_regra
  ON tipo_entidade_campo_regra (tenant_id, tipo_entidade_id, campo);
CREATE INDEX IF NOT EXISTS idx_tipo_entidade_campo_regra_tenant
  ON tipo_entidade_campo_regra (tenant_id);
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
