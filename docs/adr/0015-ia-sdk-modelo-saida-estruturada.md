# 0015. IA: SDK oficial, Claude Opus 5.5 e saída estruturada

- **Status:** Aceito
- **Data:** 2026-11-10
- **Complementa:** [ADR 0007](0007-ia-atras-de-interface.md), que deixou SDK, modelo e preço para a Fase 2

## Contexto
O ADR 0007 definiu a interface `CareerAiClient`, prompts versionados, validação por schema e o
registro em `ai_calls`, mas deixou em aberto como chamar o provedor.

## Decisão
- **SDK oficial `com.anthropic:anthropic-java` (3.3.0)**, isolado num adaptador (`AnthropicLlmGateway`).
  A regra (validar, tentar de novo, registrar) fica no `CareerAiService`, testado sem rede.
- **Modelo `claude-opus-5-5`** ($4 / $20 por milhão de tokens de entrada / saída), trocável por
  `ROTA_AI_MODEL`. Esforço `high` explícito: no Opus 5.5 o raciocínio está sempre ligado e o padrão
  seria `medium`.
- **Saída estruturada** (`output_config.format` com o JSON Schema do prompt) e validação local de novo
  com o networknt json-schema-validator. Uma resposta inválida é tentada de novo até `max-attempts`
  (2); recusa e resposta cortada não.
- **Fallback em recusa** com `fallbacks: "default"` (beta `server-side-fallback-2026-07-01`): a própria
  API refaz a chamada no modelo indicado para aquele tipo de recusa. O modelo que respondeu de fato
  é o que vai para `ai_calls`.
- **Arquivos:** `resources/prompts/<nome>/v<N>.md` e `v<N>.schema.json`. O schema fica por versão
  (e não um `schema.json` único), porque mudar o formato da saída também é uma versão nova.
- **Custo** em micro-dólares (`cost_usd_micros`, inteiro): a API cobra em USD, e centavos seriam
  grossos demais para uma chamada que custa frações de centavo. A tabela de preços fica no
  `application.yml`.

## Alternativas descartadas
- **Spring AI:** mais uma camada por cima da nossa interface, e sem acesso direto a recursos novos da API
  (fallback, esforço) quando saem.
- **HTTP direto com RestClient:** o SDK já trata retry, timeout e os tipos da resposta.

## Consequências
- Sem `ANTHROPIC_API_KEY`, a API sobe e os testes passam; só a chamada à IA falha, com mensagem clara.
- Trocar de modelo é configuração. Atualizar preços é editar o `application.yml`.
