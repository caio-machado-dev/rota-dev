# 0013. Deploy na VPS com Docker Compose

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
Já existe uma VPS (Contabo, x86_64, 6 vCPU, ~11,7 GB de RAM, ~9 GB livres no diagnóstico de 02/11/2026). Ela é compartilhada com outras aplicações em produção (Gasto Certo) e com ferramentas de desenvolvimento de outro usuário. O objetivo é não ter custo extra.

## Decisão
- Imagens Docker `linux/amd64` geradas no GitHub Actions e publicadas no GHCR. **Nunca compilar na VPS.**
- Na VPS: `docker compose pull && docker compose up -d`, com **limite de memória por container** (postgres 512M, api 768M, web 256M).
- A JVM usa `-XX:MaxRAMPercentage` para respeitar o limite do container.
- HTTPS pelo Caddy já existente. Na fase de deploy, ele sai do compose do Gasto Certo para um compose `edge` próprio, com uma rede Docker compartilhada.
- `pg_dump` diário enviado para um armazenamento externo (R2 ou B2).

## Alternativas descartadas
- **Supabase Free + Cloudflare:** custo zero, mas obrigaria o backend a ser TypeScript (Edge Functions), com limite de 150 s por execução e pausa após 7 dias sem uso.
- **Coolify/Dokploy:** painel consome 0,4–1 GB e esconde o que acontece.
- **Kubernetes (k3s):** desproporcional.

## Consequências
- O uso estimado de 0,5–0,8 GB cabe com folga.
- O deploy tem alguns segundos fora do ar. Se isso incomodar, o próximo passo natural é o Kamal, que usa as mesmas imagens.
- O pipeline de publicação e o compose de produção entram na fase de deploy. A Fase 0 cobre só o ambiente local e o CI.
