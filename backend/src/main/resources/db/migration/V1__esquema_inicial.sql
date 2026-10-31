CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuarios (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'USUARIO')),
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE espacios (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           VARCHAR(50) NOT NULL UNIQUE,
    tipo             VARCHAR(30) NOT NULL
                     CHECK (tipo IN ('ESCRITORIO', 'SALA_REUNIONES', 'SALA_CONFERENCIAS', 'CABINA')),
    capacidad        INT         NOT NULL CHECK (capacidad > 0),
    minutos_limpieza INT         NOT NULL CHECK (minutos_limpieza >= 0),
    activo           BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE reservas (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL REFERENCES usuarios (id),
    espacio_id     BIGINT      NOT NULL REFERENCES espacios (id),
    hora_inicio    TIMESTAMPTZ NOT NULL,
    hora_fin       TIMESTAMPTZ NOT NULL,
    hora_fin_total TIMESTAMPTZ NOT NULL,
    estado         VARCHAR(20) NOT NULL CHECK (estado IN ('CONFIRMADA', 'CANCELADA')),
    version        INT         NOT NULL DEFAULT 0,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reservas_fin_despues_de_inicio CHECK (hora_fin > hora_inicio),
    CONSTRAINT reservas_sin_solapamiento
        EXCLUDE USING gist (espacio_id WITH =, tstzrange(hora_inicio, hora_fin_total, '[)') WITH &&)
        WHERE (estado = 'CONFIRMADA')
);

CREATE INDEX idx_reservas_usuario ON reservas (usuario_id);
CREATE INDEX idx_reservas_espacio_inicio ON reservas (espacio_id, hora_inicio);
