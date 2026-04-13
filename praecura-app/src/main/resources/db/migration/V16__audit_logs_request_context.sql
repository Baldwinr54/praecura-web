-- V16: Enriquecer auditoría con contexto de request.
-- Agrega columnas para correlación y trazabilidad.

ALTER TABLE audit_logs
  ADD COLUMN IF NOT EXISTS request_id VARCHAR(80);

ALTER TABLE audit_logs
  ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);

ALTER TABLE audit_logs
  ADD COLUMN IF NOT EXISTS user_agent VARCHAR(300);

CREATE INDEX IF NOT EXISTS idx_audit_request_id ON audit_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_ip_address ON audit_logs(ip_address);
