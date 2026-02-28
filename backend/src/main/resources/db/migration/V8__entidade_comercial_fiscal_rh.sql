-- Entidade: comercial + fiscal + RH completo

CREATE TABLE IF NOT EXISTS tipo_frequencia_cobranca (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(80) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_frequencia_cobranca_tenant_nome
  ON tipo_frequencia_cobranca (tenant_id, lower(nome));

CREATE TABLE IF NOT EXISTS rh_tipo_funcionario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  descricao VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_tipo_funcionario_tenant_descricao
  ON rh_tipo_funcionario (tenant_id, lower(descricao));

CREATE TABLE IF NOT EXISTS rh_situacao_funcionario (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  descricao VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_situacao_funcionario_tenant_descricao
  ON rh_situacao_funcionario (tenant_id, lower(descricao));

CREATE TABLE IF NOT EXISTS rh_setor (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_setor_tenant_nome
  ON rh_setor (tenant_id, lower(nome));

CREATE TABLE IF NOT EXISTS rh_cargo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_cargo_tenant_nome
  ON rh_cargo (tenant_id, lower(nome));

CREATE TABLE IF NOT EXISTS rh_ocupacao_atividade (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  codigo VARCHAR(40),
  pais_codigo_ibge BIGINT,
  pais_nome VARCHAR(80),
  ordem INTEGER NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_ocupacao_atividade_tenant_nome
  ON rh_ocupacao_atividade (tenant_id, lower(nome));

CREATE TABLE IF NOT EXISTS rh_qualificacao (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  completo BOOLEAN NOT NULL DEFAULT FALSE,
  tipo CHAR(1),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_qualificacao_tenant_nome
  ON rh_qualificacao (tenant_id, lower(nome));

CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_tipo_funcionario_id_tenant
  ON rh_tipo_funcionario (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_situacao_funcionario_id_tenant
  ON rh_situacao_funcionario (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_setor_id_tenant
  ON rh_setor (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_cargo_id_tenant
  ON rh_cargo (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_ocupacao_atividade_id_tenant
  ON rh_ocupacao_atividade (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rh_qualificacao_id_tenant
  ON rh_qualificacao (id, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tipo_frequencia_cobranca_id_tenant
  ON tipo_frequencia_cobranca (id, tenant_id);

CREATE TABLE IF NOT EXISTS entidade_info_comercial (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  faturamento_dia_inicial DATE,
  faturamento_dia_final DATE,
  faturamento_dias_prazo INTEGER,
  boletos_enviar_email BOOLEAN NOT NULL DEFAULT FALSE,
  faturamento_frequencia_cobranca_id BIGINT,
  juro_taxa_padrao NUMERIC(5,2),
  ramo_atividade VARCHAR(100),
  consumidor_final BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_info_comercial_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ent_info_comercial_frequencia_scope
    FOREIGN KEY (faturamento_frequencia_cobranca_id, tenant_id)
    REFERENCES tipo_frequencia_cobranca (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT ck_ent_info_comercial_juro_taxa
    CHECK (juro_taxa_padrao IS NULL OR (juro_taxa_padrao >= 0 AND juro_taxa_padrao <= 100))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_info_comercial_scope_registro
  ON entidade_info_comercial (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_dados_fiscais (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  manifestar_nota_automaticamente SMALLINT,
  usa_nota_fiscal_fatura SMALLINT,
  ignorar_importacao_nota SMALLINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_dados_fiscais_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT ck_ent_dados_fiscais_flags
    CHECK (
      (manifestar_nota_automaticamente IS NULL OR manifestar_nota_automaticamente IN (0,1,2))
      AND (usa_nota_fiscal_fatura IS NULL OR usa_nota_fiscal_fatura IN (0,1,2))
      AND (ignorar_importacao_nota IS NULL OR ignorar_importacao_nota IN (0,1,2))
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_dados_fiscais_scope_registro
  ON entidade_dados_fiscais (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_contrato_rh (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  numero VARCHAR(40),
  admissao_data DATE,
  remuneracao NUMERIC(19,2),
  remuneracao_complementar NUMERIC(19,2),
  bonificacao NUMERIC(19,2),
  sindicalizado BOOLEAN NOT NULL DEFAULT FALSE,
  percentual_insalubridade NUMERIC(5,2),
  percentual_periculosidade NUMERIC(5,2),
  tipo_funcionario_id BIGINT,
  situacao_funcionario_id BIGINT,
  setor_id BIGINT,
  cargo_id BIGINT,
  ocupacao_atividade_id BIGINT,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_contrato_rh_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ent_contrato_rh_tipo_funcionario_scope
    FOREIGN KEY (tipo_funcionario_id, tenant_id)
    REFERENCES rh_tipo_funcionario (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_ent_contrato_rh_situacao_scope
    FOREIGN KEY (situacao_funcionario_id, tenant_id)
    REFERENCES rh_situacao_funcionario (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_ent_contrato_rh_setor_scope
    FOREIGN KEY (setor_id, tenant_id)
    REFERENCES rh_setor (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_ent_contrato_rh_cargo_scope
    FOREIGN KEY (cargo_id, tenant_id)
    REFERENCES rh_cargo (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_ent_contrato_rh_ocupacao_scope
    FOREIGN KEY (ocupacao_atividade_id, tenant_id)
    REFERENCES rh_ocupacao_atividade (id, tenant_id)
    ON DELETE SET NULL,
  CONSTRAINT ck_ent_contrato_rh_percentuais
    CHECK (
      (percentual_insalubridade IS NULL OR (percentual_insalubridade >= 0 AND percentual_insalubridade <= 100))
      AND (percentual_periculosidade IS NULL OR (percentual_periculosidade >= 0 AND percentual_periculosidade <= 100))
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_contrato_rh_scope_registro
  ON entidade_contrato_rh (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_info_rh (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  atividades VARCHAR(1000),
  habilidades VARCHAR(1000),
  experiencias VARCHAR(1000),
  aceita_viajar BOOLEAN NOT NULL DEFAULT FALSE,
  possui_carro BOOLEAN NOT NULL DEFAULT FALSE,
  possui_moto BOOLEAN NOT NULL DEFAULT FALSE,
  meta_media_horas_vendidas_dia BIGINT,
  meta_produtos_vendidos NUMERIC(19,2),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_info_rh_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_info_rh_scope_registro
  ON entidade_info_rh (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_referencia (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  atividades TEXT,
  data_inicio DATE,
  data_fim DATE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_referencia_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ent_referencia_scope_registro
  ON entidade_referencia (tenant_id, empresa_id, registro_entidade_id);

CREATE TABLE IF NOT EXISTS entidade_qualificacao_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  registro_entidade_id BIGINT NOT NULL,
  rh_qualificacao_id BIGINT NOT NULL,
  completo BOOLEAN NOT NULL DEFAULT FALSE,
  tipo CHAR(1),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_ent_qualificacao_registro_scope
    FOREIGN KEY (registro_entidade_id, tenant_id, empresa_id)
    REFERENCES registro_entidade (id, tenant_id, empresa_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_ent_qualificacao_rh_scope
    FOREIGN KEY (rh_qualificacao_id, tenant_id)
    REFERENCES rh_qualificacao (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ent_qualificacao_scope_unique
  ON entidade_qualificacao_item (tenant_id, empresa_id, registro_entidade_id, rh_qualificacao_id);

CREATE INDEX IF NOT EXISTS idx_ent_qualificacao_scope_registro
  ON entidade_qualificacao_item (tenant_id, empresa_id, registro_entidade_id);
