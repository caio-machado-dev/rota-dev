-- Snapshots de perfil e evidências (glossário).
--
-- Snapshot: fotografia normalizada do que uma fonte disse sobre o usuário num momento.
-- Reprocessar a fonte gera um snapshot novo; os antigos ficam como histórico.
-- Evidência: fato verificável que sustenta uma conclusão do diagnóstico ("repo X tem CI").

CREATE TABLE profile_snapshots (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source_type text        NOT NULL,
    -- Job que gerou o snapshot. UNIQUE: se o job rodar de novo, não duplica (ADR 0006).
    job_id      uuid        UNIQUE,
    -- Resposta bruta da fonte, para reprocessar no futuro sem buscar de novo (ADR 0009).
    raw         jsonb       NOT NULL,
    -- Habilidades normalizadas: [{"name": "Java", "weight": 0.62, "confidence": "HIGH"}, ...]
    skills      jsonb       NOT NULL,
    created_at  timestamptz NOT NULL
);

CREATE INDEX profile_snapshots_user_idx ON profile_snapshots (user_id, created_at DESC);

CREATE TABLE evidences (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    snapshot_id uuid        NOT NULL REFERENCES profile_snapshots (id) ON DELETE CASCADE,
    kind        text        NOT NULL,
    -- Habilidade que a evidência sustenta (ex.: "Java"). Pode ser vazia para fatos gerais.
    skill       text,
    title       text        NOT NULL,
    detail      text,
    url         text,
    confidence  text        NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at  timestamptz NOT NULL
);

CREATE INDEX evidences_snapshot_idx ON evidences (snapshot_id);
CREATE INDEX evidences_user_skill_idx ON evidences (user_id, skill);
