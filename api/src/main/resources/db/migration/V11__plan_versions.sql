-- Plano de carreira (glossário: "Plano"), versionado. Cada mudança é uma versão nova com o motivo;
-- a versão atual é a mais recente pronta. Nada é sobrescrito: o histórico mostra como o plano mudou.

CREATE TABLE plan_versions (
    id           uuid        PRIMARY KEY,
    user_id      uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    version      int         NOT NULL,
    status       text        NOT NULL CHECK (status IN ('PENDING', 'READY', 'FAILED')),
    -- Por que esta versão existe ("Primeira versão", "Reajuste aceito no check-in de 16/11"...).
    reason       text        NOT NULL,
    diagnosis_id uuid        NOT NULL REFERENCES diagnoses (id),
    -- Data de início (no fuso do usuário): a semana 1 começa na segunda-feira desta data.
    starts_on    date        NOT NULL,
    content      jsonb,
    ai_call_id   uuid        REFERENCES ai_calls (id),
    error        text,
    created_at   timestamptz NOT NULL,
    finished_at  timestamptz,
    CONSTRAINT plan_versions_user_version_uq UNIQUE (user_id, version)
);

CREATE INDEX plan_versions_user_idx ON plan_versions (user_id, version DESC);
