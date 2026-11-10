-- Objetivo de carreira (glossário): um por usuário. Mudar o objetivo sobrescreve; o histórico
-- de como o plano mudou fica nas versões do plano (Fase 2), não aqui.

CREATE TABLE career_goals (
    user_id              uuid        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    target_role          text        NOT NULL,
    target_level         text        NOT NULL CHECK (target_level IN ('JUNIOR', 'PLENO', 'SENIOR', 'ESPECIALISTA')),
    track                text        NOT NULL CHECK (track IN ('BACKEND', 'FRONTEND', 'FULLSTACK', 'DADOS', 'DEVOPS', 'MOBILE')),
    -- Data, sem hora: "até 30/06/2027" no calendário do usuário.
    deadline             date        NOT NULL,
    hours_per_week       int         NOT NULL CHECK (hours_per_week BETWEEN 1 AND 60),
    primary_stack        text,
    wants_certifications boolean     NOT NULL DEFAULT false,
    notes                text,
    created_at           timestamptz NOT NULL,
    updated_at           timestamptz NOT NULL
);
