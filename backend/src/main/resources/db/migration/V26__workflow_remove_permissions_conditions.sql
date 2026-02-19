ALTER TABLE workflow_transition
  DROP COLUMN IF EXISTS permissions_json,
  DROP COLUMN IF EXISTS conditions_json;
