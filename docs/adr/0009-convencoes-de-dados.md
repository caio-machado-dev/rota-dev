# 0009. Convenções de dados

- **Status:** Aceito
- **Data:** 2026-11-02

## Decisão
| Tema | Regra | Motivo |
|---|---|---|
| IDs | UUID v7, gerado na aplicação | Não expõe contagem como um id sequencial, e é ordenável por tempo, o que é bom para índice |
| Datas e horas | `timestamptz` no banco, `Instant` no Java, sempre UTC | Sem ambiguidade de fuso |
| "Hoje" | Calculado no **fuso do usuário** (campo `time_zone`, padrão `America/Sao_Paulo`) | O painel "Agora" e o check-in de domingo dependem do dia local |
| Dinheiro | `BIGINT` em centavos + moeda (`BRL`) | `double` arredonda errado |
| Snapshots brutos das fontes | `JSONB` | O formato do GitHub e do LinkedIn muda, e o bruto serve para reprocessar |
| Nomes no banco | `snake_case`, tabelas no plural | Padrão do Postgres |
| Migrações | Flyway, `V<n>__descricao.sql`. Migração já aplicada nunca é editada | Bancos já migrados não reaplicam a versão editada |

## Consequências
- Testes que dependem de data usam um `Clock` injetado, nunca `Instant.now()` direto.
