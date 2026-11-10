-- Índice de prontidão (glossário) registrado a cada check-in, para acompanhar a evolução.
-- Os componentes (0 a 100 cada) ficam guardados: dá para ver o que puxou o índice para cima ou para baixo.

CREATE TABLE readiness_scores (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start date        NOT NULL,
    score      smallint    NOT NULL CHECK (score BETWEEN 0 AND 100),
    skills     smallint    NOT NULL CHECK (skills BETWEEN 0 AND 100),
    gates      smallint    NOT NULL CHECK (gates BETWEEN 0 AND 100),
    study      smallint    NOT NULL CHECK (study BETWEEN 0 AND 100),
    habit      smallint    NOT NULL CHECK (habit BETWEEN 0 AND 100),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, week_start)
);
