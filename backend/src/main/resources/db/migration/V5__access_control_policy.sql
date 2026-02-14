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

