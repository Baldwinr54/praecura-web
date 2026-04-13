-- Daily metrics snapshots for dashboards/reporting
CREATE TABLE IF NOT EXISTS metrics_daily (
  day DATE PRIMARY KEY,
  total BIGINT NOT NULL DEFAULT 0,
  pendientes BIGINT NOT NULL DEFAULT 0,
  programadas BIGINT NOT NULL DEFAULT 0,
  confirmadas BIGINT NOT NULL DEFAULT 0,
  completadas BIGINT NOT NULL DEFAULT 0,
  canceladas BIGINT NOT NULL DEFAULT 0,
  no_asistio BIGINT NOT NULL DEFAULT 0,
  no_show_rate DOUBLE PRECISION NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
