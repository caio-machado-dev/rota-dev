-- Registro de toda chamada à IA (ADR 0007): quem, qual prompt e versão, qual modelo, tokens,
-- custo estimado, latência e resultado. Serve para auditar e para enxergar o custo por usuário.

CREATE TABLE ai_calls (
    id                    uuid        PRIMARY KEY,
    user_id               uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    prompt_name           text        NOT NULL,
    prompt_version        int         NOT NULL,
    -- Modelo que respondeu de fato. Pode ser diferente do pedido, se houve fallback.
    model                 text        NOT NULL,
    attempt               int         NOT NULL,
    status                text        NOT NULL CHECK (status IN ('SUCCEEDED', 'INVALID_OUTPUT', 'REFUSED', 'TRUNCATED', 'ERROR')),
    input_tokens          bigint      NOT NULL DEFAULT 0,
    output_tokens         bigint      NOT NULL DEFAULT 0,
    cache_read_tokens     bigint      NOT NULL DEFAULT 0,
    cache_write_tokens    bigint      NOT NULL DEFAULT 0,
    -- Custo em milionésimos de dólar (a API cobra em USD). Inteiro, como todo dinheiro no projeto
    -- (ADR 0009); centavos seriam grossos demais para uma chamada que custa frações de centavo.
    -- Nulo quando o modelo não está na tabela de preços da configuração.
    cost_usd_micros       bigint,
    latency_ms            bigint      NOT NULL,
    error                 text,
    created_at            timestamptz NOT NULL
);

CREATE INDEX ai_calls_user_idx ON ai_calls (user_id, created_at DESC);
