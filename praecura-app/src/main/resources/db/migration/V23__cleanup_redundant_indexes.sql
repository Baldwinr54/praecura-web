-- Limpieza de índices redundantes / de baja selectividad
-- Motivo:
-- 1) V4 creó idx_appointments_doctor_date (doctor_id, scheduled_at)
--    y V20 creó idx_appointments_doctor_scheduled (doctor_id, scheduled_at), quedando duplicado.
-- 2) Índices sobre columnas booleanas (active) suelen no aportar (baja selectividad) y añaden costo
--    en INSERT/UPDATE. Además, para citas activas existe un índice parcial más útil:
--    idx_appointments_doctor_sched_active (doctor_id, scheduled_at) WHERE active=true.
--
-- Esta migración es idempotente y segura: no cambia datos, solo elimina índices redundantes.

-- 1) Elimina el duplicado (dejamos idx_appointments_doctor_scheduled como índice compuesto "general")
DROP INDEX IF EXISTS public.idx_appointments_doctor_date;

-- 2) Elimina índices booleanos "active" (si existieran)
DROP INDEX IF EXISTS public.idx_appointments_active;
DROP INDEX IF EXISTS public.idx_doctors_active;
DROP INDEX IF EXISTS public.idx_patients_active;
