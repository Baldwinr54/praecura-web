-- Pre-carga de especialidades médicas (catálogo inicial)
-- Idempotente: si ya existen, no duplica.

INSERT INTO specialties (name, active)
SELECT v.name, true
FROM (
  VALUES
    ('Medicina General'),
    ('Medicina Familiar'),
    ('Medicina Interna'),
    ('Pediatría'),
    ('Ginecología y Obstetricia'),
    ('Cardiología'),
    ('Endocrinología'),
    ('Gastroenterología'),
    ('Neumología'),
    ('Nefrología'),
    ('Neurología'),
    ('Psiquiatría'),
    ('Psicología Clínica'),
    ('Dermatología'),
    ('Oftalmología'),
    ('Otorrinolaringología'),
    ('Traumatología y Ortopedia'),
    ('Reumatología'),
    ('Urología'),
    ('Cirugía General'),
    ('Anestesiología'),
    ('Radiología'),
    ('Medicina Física y Rehabilitación'),
    ('Nutrición y Dietética'),
    ('Infectología'),
    ('Oncología'),
    ('Hematología'),
    ('Alergología e Inmunología'),
    ('Medicina del Trabajo'),
    ('Geriatría')
) AS v(name)
WHERE NOT EXISTS (
  SELECT 1 FROM specialties s WHERE lower(s.name) = lower(v.name)
);
