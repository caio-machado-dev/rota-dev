# 0006. Fila de jobs no Postgres

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
A ingestão do GitHub, o parsing de PDF/ZIP e as chamadas de IA são lentos e podem falhar. Não devem rodar dentro da requisição HTTP e precisam de retry e idempotência.

## Decisão
Tabela `jobs` (tipo, payload JSONB, `status`, `attempts`, `run_after`, `idempotency_key UNIQUE`, `user_id`). Um worker com `@Scheduled` busca os jobs prontos com `SELECT ... FOR UPDATE SKIP LOCKED`, executa o handler do tipo e marca concluído, ou reagenda com backoff exponencial até o limite de tentativas. O resto do código só conhece a interface `JobQueue`.

## Alternativas descartadas
- **`@Async`:** o job vive só na memória. Se a API reiniciar no meio, o job some sem rastro.
- **RabbitMQ, Kafka ou SQS:** mais um serviço para operar, sem necessidade no volume atual.

## Consequências
- Os jobs sobrevivem a reinícios e ficam auditáveis no banco.
- Todo handler precisa ser **idempotente**: rodar duas vezes deve dar o mesmo resultado.
- Trocar por uma fila de verdade depois significa reimplementar `JobQueue`, sem tocar nos handlers.
- Implementado na Fase 1.
