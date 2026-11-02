# 0007. IA atrás de `CareerAiClient`

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
O diagnóstico, o plano e os reajustes dependem de um LLM. Respostas em texto livre são frágeis de interpretar, os modelos mudam e o custo precisa ser visível.

## Decisão
- Interface `CareerAiClient` no módulo `ai`. A primeira implementação usa a API da Anthropic (Claude).
- Prompts versionados em arquivo: `resources/prompts/<nome>/v<N>.md` + `schema.json`.
- Toda saída é **JSON validado contra o schema**. Se a validação falhar, a chamada falha (com retry limitado), sem tentar consertar o texto na mão.
- Toda chamada grava em `ai_calls`: usuário, prompt, versão, modelo, tokens de entrada e saída, custo estimado, latência e status.
- Toda conclusão do diagnóstico cita a evidência de onde veio. Sem evidência, o item fica como "não verificado".

## Alternativas descartadas
- **Chamar o SDK direto nos casos de uso:** acopla o domínio ao provedor.
- **Spring AI:** é uma abstração válida, mas por cima dela ainda teríamos a nossa interface. Reavaliar na Fase 2.

## Consequências
- O SDK, o modelo e o preço são escolhidos na Fase 2, conferindo a documentação vigente.
- Trocar de provedor ou de modelo vira configuração, com histórico do que cada versão de prompt produziu.
