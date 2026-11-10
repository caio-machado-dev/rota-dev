-- Diagnósticos (glossário): nível estimado por eixo, radar e lacunas, sempre citando evidências.
-- Cada pedido gera uma linha nova; o histórico mostra a evolução.

CREATE TABLE diagnoses (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status      text        NOT NULL CHECK (status IN ('PENDING', 'READY', 'FAILED')),
    -- O objetivo como estava no pedido: o diagnóstico continua legível se o objetivo mudar.
    goal        jsonb       NOT NULL,
    -- Resultado já conferido pelo domínio (eixos, lacunas, resumo). Nulo enquanto PENDING.
    result      jsonb,
    ai_call_id  uuid        REFERENCES ai_calls (id),
    error       text,
    created_at  timestamptz NOT NULL,
    finished_at timestamptz
);

CREATE INDEX diagnoses_user_idx ON diagnoses (user_id, created_at DESC);
