# 0004. Multi-tenant por linha (`user_id`)

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
Hoje há um usuário. O destino é SaaS. Vazar dado de um usuário para outro é o pior bug possível no produto.

## Decisão
Toda tabela com dado de usuário tem `user_id NOT NULL` com índice. Todo método de repositório que lê dado de usuário recebe o `userId`, e esse `userId` **sempre vem da sessão autenticada**, nunca do corpo ou da URL da requisição. Recurso de outro usuário responde **404** (e não 403, para não revelar que o recurso existe).

## Alternativas descartadas
- **Schema ou banco por tenant:** complexidade de migração e operação desproporcional.
- **Row Level Security (RLS) do Postgres já no MVP:** é um reforço excelente, mas exige propagar o usuário para a conexão (`SET app.user_id`) em todo request. Fica registrado como evolução para a fase SaaS.

## Consequências
- Cada endpoint novo ganha um teste de isolamento: o usuário A tenta acessar um recurso do B e recebe 404.
- A exclusão completa (LGPD) vira um `DELETE ... WHERE user_id = ?` por tabela, em ordem.
