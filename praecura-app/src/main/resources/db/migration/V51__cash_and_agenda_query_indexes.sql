-- Optimización de consultas críticas (caja, agenda y cuentas por cobrar).
-- Índices parciales/combinados para reducir latencia en filtros de alto uso.

CREATE INDEX IF NOT EXISTS idx_appointments_active_scheduled_id
  ON appointments(scheduled_at, id)
  WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_appointments_active_site_scheduled_id
  ON appointments(site_id, scheduled_at, id)
  WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_invoices_open_created_id
  ON invoices(created_at, id)
  WHERE status <> 'VOID' AND balance > 0;

CREATE INDEX IF NOT EXISTS idx_invoices_appointment_non_void_created_id
  ON invoices(appointment_id, created_at DESC, id DESC)
  WHERE status <> 'VOID';
