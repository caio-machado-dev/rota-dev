-- Reajustes (glossário): proposta de mudança no plano a partir do check-in, que o usuário aceita ou recusa.
-- Aceitar gera uma versão nova do plano com o motivo; recusar só registra a decisão.

ALTER TABLE plan_versions ADD COLUMN guidance text;
COMMENT ON COLUMN plan_versions.guidance IS 'Orientação extra para a IA ao montar esta versão (ex.: reduzir a carga semanal).';

CREATE TABLE plan_adjustments (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Semana do check-in que originou a proposta.
    week_start      date        NOT NULL,
    kind            text        NOT NULL CHECK (kind IN ('REDUCE_LOAD', 'INCREASE_LOAD', 'MORE_REVIEW', 'LIGHTER_WEEK')),
    reason          text        NOT NULL,
    guidance        text        NOT NULL,
    status          text        NOT NULL CHECK (status IN ('PROPOSED', 'ACCEPTED', 'REJECTED')),
    plan_version_id uuid        REFERENCES plan_versions (id),
    created_at      timestamptz NOT NULL,
    decided_at      timestamptz,
    -- No máximo uma proposta por check-in.
    CONSTRAINT plan_adjustments_user_week_uq UNIQUE (user_id, week_start)
);
