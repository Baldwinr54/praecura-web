-- Prevent duplicate active specialties by name (case-insensitive, trimmed)
-- First deactivate duplicates (keeping the lowest id as the canonical active row)
WITH dups AS (
  SELECT lower(btrim(name)) AS k, min(id) AS keep_id
  FROM specialties
  WHERE active = true AND name IS NOT NULL AND btrim(name) <> ''
  GROUP BY lower(btrim(name))
  HAVING count(*) > 1
)
UPDATE specialties s
SET active = false
FROM dups d
WHERE s.active = true
  AND s.name IS NOT NULL
  AND lower(btrim(s.name)) = d.k
  AND s.id <> d.keep_id;

-- Unique partial index (active only)
CREATE UNIQUE INDEX IF NOT EXISTS ux_specialties_name_ci_active
  ON specialties (lower(btrim(name)))
  WHERE active = true;
