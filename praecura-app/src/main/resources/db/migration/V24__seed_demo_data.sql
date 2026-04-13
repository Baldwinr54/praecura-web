-- Demo dataset (30 registros) para facilitar pruebas de UI.
-- Reglas: NO crea usuarios; NO toca especialidades (las usa como catálogo).

-- Limpieza/normalización defensiva si quedó un nombre literal por error de plantilla.
UPDATE medical_services
SET name = 'Consulta'
WHERE name = 'a.service.name';

-- 1) Pacientes (hasta 30 demo)
INSERT INTO patients (full_name, phone, cedula, active)
SELECT
  'Paciente Demo ' || lpad(i::text, 2, '0') AS full_name,
  '809-555-' || lpad((1000 + i)::text, 4, '0') AS phone,
  '402' || lpad((10000000 + i)::text, 8, '0') AS cedula,
  TRUE AS active
FROM generate_series(1, 30) AS i
WHERE NOT EXISTS (
  SELECT 1 FROM patients p WHERE p.cedula = ('402' || lpad((10000000 + i)::text, 8, '0'))
);

-- 2) Servicios (hasta 30 demo)
INSERT INTO medical_services (name, duration_minutes, price, active)
SELECT
  'Servicio Demo ' || lpad(i::text, 2, '0') AS name,
  CASE
    WHEN (i % 5) = 0 THEN 60
    WHEN (i % 5) = 1 THEN 30
    WHEN (i % 5) = 2 THEN 45
    WHEN (i % 5) = 3 THEN 20
    ELSE 15
  END AS duration_minutes,
  (500 + (i * 50))::numeric(10,2) AS price,
  TRUE AS active
FROM generate_series(1, 30) AS i
WHERE NOT EXISTS (
  SELECT 1 FROM medical_services s WHERE s.name = ('Servicio Demo ' || lpad(i::text, 2, '0'))
);

-- 3) Médicos (hasta 30 demo) usando especialidades existentes
WITH specs AS (
  SELECT id, name, row_number() OVER (ORDER BY id) AS rn
  FROM specialties
),
sc AS (
  SELECT count(*)::int AS cnt FROM specs
)
INSERT INTO doctors (full_name, specialty, specialty_id, license_no, phone, active)
SELECT
  'Dr. Demo ' || lpad(i::text, 2, '0') AS full_name,
  sp.name AS specialty,
  sp.id AS specialty_id,
  'LIC-DEMO-' || lpad(i::text, 4, '0') AS license_no,
  '809-777-' || lpad((2000 + i)::text, 4, '0') AS phone,
  TRUE AS active
FROM generate_series(1, 30) AS i
CROSS JOIN sc
JOIN specs sp
  ON sp.rn = (((i - 1) % sc.cnt) + 1)
WHERE NOT EXISTS (
  SELECT 1 FROM doctors d WHERE d.license_no = ('LIC-DEMO-' || lpad(i::text, 4, '0'))
);

-- 4) Relación médico-servicios (3 servicios por médico) para que “Servicios que ofrece” tenga datos
WITH d AS (
  SELECT id, row_number() OVER (ORDER BY id) AS rn
  FROM doctors
  WHERE license_no LIKE 'LIC-DEMO-%'
),
sv AS (
  SELECT id, row_number() OVER (ORDER BY id) AS rn
  FROM medical_services
  WHERE name LIKE 'Servicio Demo %'
),
svc_count AS (
  SELECT count(*)::int AS cnt FROM sv
),
pairs AS (
  SELECT
    d.id AS doctor_id,
    (((d.rn + off) - 1) % c.cnt + 1) AS svc_rn
  FROM d
  CROSS JOIN generate_series(0, 2) AS off
  CROSS JOIN svc_count c
)
INSERT INTO doctor_services (doctor_id, service_id)
SELECT p.doctor_id, sv.id
FROM pairs p
JOIN sv ON sv.rn = p.svc_rn
WHERE NOT EXISTS (
  SELECT 1
  FROM doctor_services ds
  WHERE ds.doctor_id = p.doctor_id AND ds.service_id = sv.id
);

-- 5) Citas (hasta 30 demo) enlazando pacientes/médicos/servicios
WITH p AS (
  SELECT id, row_number() OVER (ORDER BY id) AS rn
  FROM patients
  WHERE cedula LIKE '402%'
),
d AS (
  SELECT id, row_number() OVER (ORDER BY id) AS rn
  FROM doctors
  WHERE license_no LIKE 'LIC-DEMO-%'
),
sv AS (
  SELECT id, duration_minutes, row_number() OVER (ORDER BY id) AS rn
  FROM medical_services
  WHERE name LIKE 'Servicio Demo %'
),
base AS (
  SELECT
    i AS rn,
    p.id AS patient_id,
    d.id AS doctor_id,
    sv.id AS service_id,
    sv.duration_minutes AS duration_minutes
  FROM generate_series(1, 30) i
  JOIN p  ON p.rn  = i
  JOIN d  ON d.rn  = i
  JOIN sv ON sv.rn = i
)
INSERT INTO appointments (
  patient_id, doctor_id, service_id, scheduled_at, duration_minutes, status, notes, active
)
SELECT
  b.patient_id,
  b.doctor_id,
  b.service_id,
  (date_trunc('day', now()) + (b.rn - 1) * interval '1 day' + interval '10 hours' + ((b.rn % 6) * interval '30 minutes'))::timestamp AS scheduled_at,
  b.duration_minutes,
  CASE
    WHEN (b.rn % 10) = 0 THEN 'CANCELADA'
    WHEN (b.rn % 7) = 0 THEN 'COMPLETADA'
    ELSE 'PROGRAMADA'
  END AS status,
  'Cita demo #' || lpad(b.rn::text, 2, '0') AS notes,
  TRUE AS active
FROM base b
WHERE NOT EXISTS (
  SELECT 1
  FROM appointments a
  WHERE a.doctor_id = b.doctor_id
    AND a.scheduled_at = (date_trunc('day', now()) + (b.rn - 1) * interval '1 day' + interval '10 hours' + ((b.rn % 6) * interval '30 minutes'))::timestamp
);
