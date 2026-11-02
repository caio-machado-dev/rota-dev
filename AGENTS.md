# Rota Dev: convenções do projeto

Plataforma de plano de carreira para devs. Monorepo: `api/` (Spring Boot 4.1, Java 25),
`web/` (Next.js App Router, TypeScript), `infra/` (Docker Compose), `docs/` (ADRs, glossário).
Antes de decidir arquitetura, leia `docs/adr/`. Termos de domínio: `docs/glossario.md`.

## Quem é o usuário deste repo
O Caio vem de Python e está aprendendo Java/Spring com este projeto. É o primeiro backend
Java grande dele, e ele lê e revisa cada linha. Trabalhe como um pleno que ensina:
- Ao usar anotação, transação ou padrão Spring não óbvio, explique o porquê em 2–3 linhas
  (no PR ou num comentário, se a explicação pertence ao código). Compare com o equivalente
  em Python/Django/FastAPI quando ajudar.
- Prefira código direto e legível a abstração esperta. Uma classe nova por vez.
- Quando uma parte pequena e didática couber a ele (um caso de uso, um teste), deixe
  `// TODO(caio): <o que fazer e critério de pronto>`, com o esqueleto compilando e o build verde.
- Ao discordar de um pedido, proponha a alternativa antes de implementar.
- Trabalhe por fase do roadmap e pare ao fim de cada uma para revisão.

## Backend (api/)
- Pacote base `dev.rotadev`. Cada pacote direto abaixo dele é um módulo do Spring Modulith,
  com as camadas `domain/` (regra pura, sem Spring/JPA), `application/` (casos de uso,
  `@Transactional` aqui), `infrastructure/` (JPA, HTTP externo) e `web/` (controllers + records).
  `ModularityTest` verifica as fronteiras. Detalhes: ADR 0002.
- A API expõe só records. Entidades JPA ficam em `infrastructure/`.
- Toda tabela de dado de usuário tem `user_id`, e o `userId` vem da sessão autenticada.
  Cada endpoint novo ganha um teste de isolamento entre usuários (A acessa recurso de B → 404).
- Schema muda só por migração Flyway nova (`V<n>__descricao.sql`). Migração aplicada é imutável.
- Dinheiro em centavos (`long`) + BRL. Datas em `Instant`/`timestamptz`; "hoje" usa o fuso do
  usuário e um `Clock` injetado.
- Chamadas de IA passam por `CareerAiClient`, com prompt versionado e saída validada por JSON Schema.
- Testes: JUnit 5 + AssertJ + Mockito para domínio e aplicação; Testcontainers (Postgres)
  para repositório e integração.

## Frontend (web/)
- pnpm. Tema escuro padrão, Geist / Geist Mono, shadcn/ui, Recharts.
- Nome, slug e cores só vêm de `src/config/brand.ts`.
- Chamadas à API usam caminho relativo `/api/...` (o Next faz rewrite para a API, ADR 0005).

## Comandos
- Ambiente local completo: `docker compose -f infra/compose.yaml up --build`
- Só o banco, para rodar API e web fora do Docker: `docker compose -f infra/compose.yaml up postgres`
- API (em `api/`): `./mvnw verify` roda tudo; `./mvnw test -Dtest=NomeDoTeste` roda um.
  Os testes de integração precisam do Docker rodando (Testcontainers).
- Web (em `web/`): `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`.
- Na máquina do Caio, comandos pesados passam pela fila: `em-fila ./mvnw verify`,
  `em-fila pnpm build`, `vitest-fila <arquivo>`. Rode só os testes ligados à mudança.

## Git
- Conventional Commits em português (`feat(sources): ...`, `fix: ...`, `docs(adr): ...`),
  um commit por mudança lógica.
- Um PR por funcionalidade, a partir de branch. A `main` só recebe merge com CI verde.
