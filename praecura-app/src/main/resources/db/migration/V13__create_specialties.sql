-- Catálogo de especialidades (módulo nuevo)

CREATE TABLE IF NOT EXISTS specialties (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Evita duplicados por mayúsculas/minúsculas
CREATE UNIQUE INDEX IF NOT EXISTS uk_specialties_lower_name ON specialties (lower(name));

-- Índice útil para listados
CREATE INDEX IF NOT EXISTS idx_specialties_active ON specialties (active);
