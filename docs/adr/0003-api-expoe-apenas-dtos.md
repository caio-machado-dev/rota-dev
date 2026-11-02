# 0003. A API expõe apenas records/DTOs

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
Serializar entidades JPA direto no JSON acopla o banco ao contrato da API, dispara lazy loading fora da transação e pode vazar campos sensíveis (ex.: tokens).

## Decisão
Controllers recebem e devolvem **records Java** definidos na camada `web` do módulo. Entidades JPA não saem de `infrastructure`. O mapeamento é escrito à mão.

## Alternativas descartadas
- **Expor entidades:** os problemas acima.
- **MapStruct:** economiza código, mas esconde o mapeamento atrás de geração de código. Reavaliar quando o mapeamento manual virar repetição dolorosa.

## Consequências
- Mais código de mapeamento, porém explícito e fácil de ler e testar.
- Mudar o banco não quebra o contrato da API, e o contrário também vale.
