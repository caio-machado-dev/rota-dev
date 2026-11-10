-- Arquivos enviados pelo usuário: export do LinkedIn (ZIP ou PDF) e currículo (PDF ou DOCX).
-- O conteúdo fica no FileStorage (ADR 0008); aqui só os metadados.

CREATE TABLE source_files (
    id            uuid        PRIMARY KEY,
    user_id       uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind          text        NOT NULL CHECK (kind IN ('LINKEDIN', 'RESUME')),
    format        text        NOT NULL CHECK (format IN ('PDF', 'DOCX', 'ZIP')),
    -- Só para exibir. Nunca é usado como caminho no disco.
    original_name text        NOT NULL,
    size_bytes    bigint      NOT NULL,
    -- SHA-256 do conteúdo: o mesmo arquivo enviado duas vezes é reconhecido.
    sha256        text        NOT NULL,
    storage_key   text        NOT NULL,
    status        text        NOT NULL CHECK (status IN ('RECEIVED', 'PROCESSED', 'FAILED')),
    created_at    timestamptz NOT NULL,
    CONSTRAINT source_files_user_sha_uq UNIQUE (user_id, sha256)
);

CREATE INDEX source_files_user_idx ON source_files (user_id, created_at DESC);
