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
