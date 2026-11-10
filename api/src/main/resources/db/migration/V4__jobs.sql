-- Fila de jobs no próprio Postgres (ADR 0006).
--
-- O worker pega os jobs prontos com SELECT ... FOR UPDATE SKIP LOCKED: duas instâncias da API
-- nunca pegam o mesmo job, e um job travado por uma não bloqueia a outra.

CREATE TABLE jobs (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type            text        NOT NULL,
    payload         jsonb       NOT NULL,
    status          text        NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    attempts        int         NOT NULL DEFAULT 0,
    max_attempts    int         NOT NULL,
    -- Só roda a partir deste instante. O backoff de retry empurra esta data para frente.
    run_after       timestamptz NOT NULL,
    -- Quando o worker pegou o job. Um RUNNING antigo demais é de um worker que morreu no meio.
    locked_at       timestamptz,
    last_error      text,
    -- Enfileirar duas vezes com a mesma chave não cria um segundo job.
    idempotency_key text        NOT NULL UNIQUE,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL
);

-- Índice parcial: só as linhas PENDING entram, então a busca de jobs prontos continua rápida
-- mesmo com milhões de jobs concluídos na tabela.
CREATE INDEX jobs_ready_idx ON jobs (run_after) WHERE status = 'PENDING';
CREATE INDEX jobs_user_id_idx ON jobs (user_id);
