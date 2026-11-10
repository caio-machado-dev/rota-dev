-- Check-in semanal (glossário) e portões alcançados.

-- Revisão de domingo: horas da semana, revisões de diff, commits e bloqueios. Os números de horas e
-- revisões são calculados dos registros da semana e congelados aqui; commits e bloqueios a pessoa informa.
CREATE TABLE weekly_checkins (
    user_id        uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Segunda-feira da semana revisada.
    week_start     date        NOT NULL,
    minutes_logged int         NOT NULL,
    review_days    int         NOT NULL,
    commits        int         NOT NULL CHECK (commits >= 0),
    -- Como foi a semana, de 1 (péssima) a 5 (ótima).
    energy         smallint    NOT NULL CHECK (energy BETWEEN 1 AND 5),
    blockers       text,
    notes          text,
    created_at     timestamptz NOT NULL,
    PRIMARY KEY (user_id, week_start)
);

-- Portão (glossário) que a pessoa declara ter passado, com o link que prova. Fica ligado ao título
-- do portão na versão do plano em que ele estava; uma versão nova do plano não apaga a conquista.
CREATE TABLE milestone_achievements (
    id             uuid        PRIMARY KEY,
    user_id        uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_version   int         NOT NULL,
    milestone      text        NOT NULL,
    evidence_url   text        NOT NULL,
    achieved_on    date        NOT NULL,
    created_at     timestamptz NOT NULL,
    CONSTRAINT milestone_achievements_uq UNIQUE (user_id, milestone)
);
