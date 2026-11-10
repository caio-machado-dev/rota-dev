-- Usuários do Rota Dev. Por enquanto só existe login com GitHub (ADR 0005), então
-- o github_id é a chave natural: o mesmo usuário do GitHub nunca vira duas linhas.
--
-- Esta é a única tabela de dado de usuário sem coluna user_id: a própria linha é o usuário.
-- As demais tabelas (fontes, plano...) apontam para cá com user_id NOT NULL (ADR 0004).

CREATE TABLE users (
    -- UUID v7 gerado na aplicação, não no banco (ADR 0009).
    id          uuid        PRIMARY KEY,
    github_id   bigint      NOT NULL UNIQUE,
    login       text        NOT NULL,
    -- Nome, e-mail e avatar são opcionais no GitHub.
    name        text,
    email       text,
    avatar_url  text,
    -- Fuso usado para calcular o "hoje" do usuário (ADR 0009).
    time_zone   text        NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL
);
