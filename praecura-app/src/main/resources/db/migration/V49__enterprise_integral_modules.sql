CREATE TABLE IF NOT EXISTS insurance_payers (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  rnc VARCHAR(20),
  contact_phone VARCHAR(40),
  contact_email VARCHAR(160),
  notes VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS insurance_plans (
  id BIGSERIAL PRIMARY KEY,
  payer_id BIGINT NOT NULL REFERENCES insurance_payers(id),
  plan_code VARCHAR(40) NOT NULL,
  name VARCHAR(160) NOT NULL,
  coverage_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
  copay_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
  deductible_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  requires_authorization BOOLEAN NOT NULL DEFAULT false,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_insurance_plan UNIQUE (payer_id, plan_code)
);

CREATE TABLE IF NOT EXISTS patient_insurance_coverages (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id),
  plan_id BIGINT NOT NULL REFERENCES insurance_plans(id),
  policy_number VARCHAR(80),
  affiliate_number VARCHAR(80),
  valid_from DATE,
  valid_to DATE,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_patient_plan_policy UNIQUE (patient_id, plan_id, policy_number)
);

CREATE TABLE IF NOT EXISTS insurance_authorizations (
  id BIGSERIAL PRIMARY KEY,
  coverage_id BIGINT NOT NULL REFERENCES patient_insurance_coverages(id),
  appointment_id BIGINT REFERENCES appointments(id),
  authorization_number VARCHAR(80),
  status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
  requested_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  approved_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  requested_at TIMESTAMP NOT NULL DEFAULT now(),
  expires_at TIMESTAMP,
  notes VARCHAR(500),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS insurance_claims (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id),
  coverage_id BIGINT REFERENCES patient_insurance_coverages(id),
  authorization_id BIGINT REFERENCES insurance_authorizations(id),
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  claimed_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  approved_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  denied_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  denial_reason VARCHAR(500),
  submitted_at TIMESTAMP,
  resolved_at TIMESTAMP,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_insurance_claim_invoice UNIQUE (invoice_id)
);

CREATE TABLE IF NOT EXISTS clinical_encounters (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id),
  appointment_id BIGINT REFERENCES appointments(id),
  doctor_id BIGINT REFERENCES doctors(id),
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  encounter_at TIMESTAMP NOT NULL DEFAULT now(),
  chief_complaint VARCHAR(400),
  subjective TEXT,
  objective TEXT,
  assessment TEXT,
  plan TEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS encounter_diagnoses (
  id BIGSERIAL PRIMARY KEY,
  encounter_id BIGINT NOT NULL REFERENCES clinical_encounters(id) ON DELETE CASCADE,
  icd10_code VARCHAR(20),
  description VARCHAR(400) NOT NULL,
  primary_diagnosis BOOLEAN NOT NULL DEFAULT false,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS clinical_orders (
  id BIGSERIAL PRIMARY KEY,
  encounter_id BIGINT NOT NULL REFERENCES clinical_encounters(id) ON DELETE CASCADE,
  order_type VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ORDERED',
  priority VARCHAR(30) NOT NULL DEFAULT 'ROUTINE',
  order_name VARCHAR(220) NOT NULL,
  instructions VARCHAR(600),
  due_at TIMESTAMP,
  cost_estimate NUMERIC(12,2) NOT NULL DEFAULT 0,
  result_summary TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inpatient_beds (
  id BIGSERIAL PRIMARY KEY,
  site_id BIGINT NOT NULL REFERENCES clinic_sites(id),
  code VARCHAR(40) NOT NULL,
  ward VARCHAR(120),
  bed_type VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
  status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE',
  active BOOLEAN NOT NULL DEFAULT true,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_inpatient_bed UNIQUE (site_id, code)
);

CREATE TABLE IF NOT EXISTS patient_admissions (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id),
  bed_id BIGINT REFERENCES inpatient_beds(id),
  doctor_id BIGINT REFERENCES doctors(id),
  status VARCHAR(40) NOT NULL DEFAULT 'ADMITTED',
  admitted_at TIMESTAMP NOT NULL DEFAULT now(),
  expected_discharge_at TIMESTAMP,
  discharged_at TIMESTAMP,
  admission_reason VARCHAR(500),
  discharge_summary TEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS surgery_schedules (
  id BIGSERIAL PRIMARY KEY,
  admission_id BIGINT REFERENCES patient_admissions(id),
  patient_id BIGINT NOT NULL REFERENCES patients(id),
  doctor_id BIGINT REFERENCES doctors(id),
  site_id BIGINT REFERENCES clinic_sites(id),
  resource_id BIGINT REFERENCES clinic_resources(id),
  status VARCHAR(40) NOT NULL DEFAULT 'SCHEDULED',
  procedure_name VARCHAR(220) NOT NULL,
  anesthesia_type VARCHAR(120),
  scheduled_at TIMESTAMP NOT NULL,
  estimated_minutes INTEGER NOT NULL DEFAULT 60,
  started_at TIMESTAMP,
  ended_at TIMESTAMP,
  notes VARCHAR(600),
  created_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS nursing_notes (
  id BIGSERIAL PRIMARY KEY,
  admission_id BIGINT NOT NULL REFERENCES patient_admissions(id) ON DELETE CASCADE,
  shift VARCHAR(30) NOT NULL DEFAULT 'AM',
  recorded_at TIMESTAMP NOT NULL DEFAULT now(),
  recorded_by VARCHAR(120),
  notes TEXT NOT NULL,
  vitals_snapshot VARCHAR(500),
  medication_administered VARCHAR(500),
  adverse_event BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS inventory_items (
  id BIGSERIAL PRIMARY KEY,
  sku VARCHAR(60) UNIQUE,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100),
  presentation VARCHAR(120),
  unit VARCHAR(30) NOT NULL DEFAULT 'UNIDAD',
  cost_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  sale_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(6,4) NOT NULL DEFAULT 0,
  min_stock NUMERIC(12,2) NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stock_movements (
  id BIGSERIAL PRIMARY KEY,
  item_id BIGINT NOT NULL REFERENCES inventory_items(id),
  movement_type VARCHAR(40) NOT NULL,
  quantity NUMERIC(12,2) NOT NULL,
  unit_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
  reference_type VARCHAR(60),
  reference_id BIGINT,
  lot_number VARCHAR(80),
  expires_at DATE,
  notes VARCHAR(500),
  created_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS purchase_orders (
  id BIGSERIAL PRIMARY KEY,
  supplier_name VARCHAR(180) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  ordered_at TIMESTAMP NOT NULL DEFAULT now(),
  received_at TIMESTAMP,
  subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
  tax NUMERIC(12,2) NOT NULL DEFAULT 0,
  total NUMERIC(12,2) NOT NULL DEFAULT 0,
  notes VARCHAR(500),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
  id BIGSERIAL PRIMARY KEY,
  purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
  item_id BIGINT NOT NULL REFERENCES inventory_items(id),
  quantity NUMERIC(12,2) NOT NULL,
  received_quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
  unit_cost NUMERIC(12,2) NOT NULL,
  tax_rate NUMERIC(6,4) NOT NULL DEFAULT 0,
  total NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pharmacy_dispensations (
  id BIGSERIAL PRIMARY KEY,
  patient_id BIGINT NOT NULL REFERENCES patients(id),
  admission_id BIGINT REFERENCES patient_admissions(id),
  encounter_id BIGINT REFERENCES clinical_encounters(id),
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  dispensed_at TIMESTAMP,
  total NUMERIC(12,2) NOT NULL DEFAULT 0,
  notes VARCHAR(500),
  created_by VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pharmacy_dispensation_items (
  id BIGSERIAL PRIMARY KEY,
  dispensation_id BIGINT NOT NULL REFERENCES pharmacy_dispensations(id) ON DELETE CASCADE,
  item_id BIGINT NOT NULL REFERENCES inventory_items(id),
  quantity NUMERIC(12,2) NOT NULL,
  unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
  total NUMERIC(12,2) NOT NULL DEFAULT 0,
  notes VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS system_permissions (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  module VARCHAR(60) NOT NULL,
  name VARCHAR(140) NOT NULL,
  description VARCHAR(300),
  critical BOOLEAN NOT NULL DEFAULT false,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_permissions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES system_permissions(id) ON DELETE CASCADE,
  granted BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uk_user_permission UNIQUE (user_id, permission_id)
);

CREATE TABLE IF NOT EXISTS critical_action_approvals (
  id BIGSERIAL PRIMARY KEY,
  action_code VARCHAR(80) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(500),
  requested_by_user_id BIGINT NOT NULL REFERENCES users(id),
  approved_by_user_id BIGINT REFERENCES users(id),
  used_by_user_id BIGINT REFERENCES users(id),
  requested_at TIMESTAMP NOT NULL DEFAULT now(),
  approved_at TIMESTAMP,
  used_at TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS operational_alerts (
  id BIGSERIAL PRIMARY KEY,
  alert_type VARCHAR(60) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
  title VARCHAR(160) NOT NULL,
  message VARCHAR(600) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  metadata_json TEXT,
  detected_at TIMESTAMP NOT NULL DEFAULT now(),
  resolved_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_operational_closings (
  id BIGSERIAL PRIMARY KEY,
  closing_date DATE NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  total_appointments BIGINT NOT NULL DEFAULT 0,
  completed_appointments BIGINT NOT NULL DEFAULT 0,
  total_collected NUMERIC(14,2) NOT NULL DEFAULT 0,
  total_pending NUMERIC(14,2) NOT NULL DEFAULT 0,
  total_refunds NUMERIC(14,2) NOT NULL DEFAULT 0,
  open_alerts BIGINT NOT NULL DEFAULT 0,
  notes VARCHAR(500),
  generated_at TIMESTAMP NOT NULL DEFAULT now(),
  generated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_insurance_claims_status ON insurance_claims(status);
CREATE INDEX IF NOT EXISTS idx_insurance_authorizations_status ON insurance_authorizations(status);
CREATE INDEX IF NOT EXISTS idx_clinical_encounters_patient ON clinical_encounters(patient_id, encounter_at DESC);
CREATE INDEX IF NOT EXISTS idx_clinical_orders_status ON clinical_orders(status, due_at);
CREATE INDEX IF NOT EXISTS idx_admissions_status ON patient_admissions(status, admitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_surgery_status_date ON surgery_schedules(status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_stock_movements_item_date ON stock_movements(item_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders(status, ordered_at DESC);
CREATE INDEX IF NOT EXISTS idx_dispensations_status ON pharmacy_dispensations(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_approvals_pending ON critical_action_approvals(status, action_code, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_operational_alerts_status ON operational_alerts(status, severity, detected_at DESC);

INSERT INTO system_permissions (code, module, name, description, critical)
VALUES
  ('INSURANCE_MANAGE', 'INSURANCE', 'Gestionar ARS y coberturas', 'Configurar aseguradoras, planes, coberturas y reclamos.', false),
  ('CLINICAL_ENCOUNTER_MANAGE', 'CLINICAL', 'Gestionar encuentros SOAP', 'Crear y cerrar encuentros clínicos con SOAP.', false),
  ('CLINICAL_ORDER_MANAGE', 'CLINICAL', 'Gestionar órdenes clínicas', 'Ordenar laboratorio, imágenes, procedimientos y recetas.', false),
  ('INPATIENT_MANAGE', 'INPATIENT', 'Gestionar hospitalización', 'Admisiones, camas, cirugías y enfermería.', false),
  ('PHARMACY_MANAGE', 'PHARMACY', 'Gestionar farmacia', 'Dispensación e inventario farmacéutico.', false),
  ('INVENTORY_MANAGE', 'INVENTORY', 'Gestionar inventario y compras', 'Entradas, salidas y órdenes de compra.', false),
  ('REPORT_EXECUTIVE_VIEW', 'REPORTS', 'Ver BI ejecutivo', 'Indicadores ejecutivos, alertas y cierres diarios.', false),
  ('ALERTS_MANAGE', 'REPORTS', 'Gestionar alertas operativas', 'Confirmar y resolver alertas.', false),
  ('DAILY_CLOSING_FINALIZE', 'REPORTS', 'Finalizar cierres diarios', 'Confirmar cierre diario operacional.', true),
  ('USER_PERMISSION_MANAGE', 'SECURITY', 'Gestionar permisos', 'Asignar permisos por usuario y revisar matriz.', true),
  ('APPROVAL_MANAGE', 'SECURITY', 'Aprobar acciones críticas', 'Aprobar/rechazar operaciones críticas.', true),
  ('BILLING_VOID_INVOICE', 'BILLING', 'Anular factura', 'Permite anulación de facturas.', true),
  ('BILLING_REFUND', 'BILLING', 'Registrar reembolso', 'Permite registrar reembolsos.', true),
  ('BILLING_CREDIT_NOTE', 'BILLING', 'Crear nota de crédito', 'Permite emitir notas de crédito.', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO user_permissions (user_id, permission_id, granted, created_at, updated_at)
SELECT u.id, p.id, true, now(), now()
FROM users u
JOIN roles r ON r.id = u.role_id
CROSS JOIN system_permissions p
WHERE upper(r.name) like '%ADMIN%' OR lower(u.username) = 'admin'
ON CONFLICT (user_id, permission_id) DO NOTHING;
