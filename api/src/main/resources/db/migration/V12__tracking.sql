-- Acompanhamento (Fase 3): semana padrão, sessões de estudo e o protocolo anti vibe coding.
-- "day" é sempre a data no fuso do usuário (ADR 0009), calculada pela aplicação.

-- Semana padrão (glossário): quanto tempo de cada atividade em cada dia da semana.
CREATE TABLE weekly_template_slots (
    user_id  uuid     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- 1 = segunda ... 7 = domingo (ISO 8601, igual ao java.time.DayOfWeek).
    weekday  smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    kind     text     NOT NULL CHECK (kind IN ('STUDY', 'BUILD', 'REVIEW', 'CERT')),
    minutes  int      NOT NULL CHECK (minutes BETWEEN 5 AND 600),
    PRIMARY KEY (user_id, weekday, kind)
);

-- Sessão de estudo (glossário): tipo, dia, minutos, tema e uma linha do que aprendeu.
CREATE TABLE study_sessions (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day        date        NOT NULL,
    kind       text        NOT NULL CHECK (kind IN ('STUDY', 'BUILD', 'REVIEW', 'CERT')),
    minutes    int         NOT NULL CHECK (minutes BETWEEN 5 AND 600),
    topic      text        NOT NULL,
    learning   text,
    created_at timestamptz NOT NULL
);

CREATE INDEX study_sessions_user_day_idx ON study_sessions (user_id, day);

-- Protocolo anti vibe coding (glossário): um registro por dia com os quatro passos.
CREATE TABLE review_habits (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day        date        NOT NULL,
    read_diff  boolean     NOT NULL DEFAULT false,
    explained  boolean     NOT NULL DEFAULT false,
    tested     boolean     NOT NULL DEFAULT false,
    note       text,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, day)
);
