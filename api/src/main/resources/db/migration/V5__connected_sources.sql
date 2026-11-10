-- Fontes conectadas de cada usuário (glossário: "Fonte"). Por enquanto só o GitHub tem
-- credencial; LinkedIn e currículo entram como arquivos enviados, em outra tabela.

CREATE TABLE connected_sources (
    id                     uuid        PRIMARY KEY,
    user_id                uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type                   text        NOT NULL CHECK (type IN ('GITHUB', 'LINKEDIN', 'RESUME', 'MANUAL')),
    external_login         text,
    -- Token OAuth cifrado com AES-GCM pela aplicação ("v1:..."), nunca em texto puro (ADR 0008).
    access_token_encrypted text,
    scopes                 text,
    connected_at           timestamptz NOT NULL,
    updated_at             timestamptz NOT NULL,
    last_synced_at         timestamptz,
    -- Uma fonte de cada tipo por usuário.
    CONSTRAINT connected_sources_user_type_uq UNIQUE (user_id, type)
);
