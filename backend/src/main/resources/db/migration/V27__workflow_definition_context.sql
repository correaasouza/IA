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
