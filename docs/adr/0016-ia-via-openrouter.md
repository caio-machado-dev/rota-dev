# 0016. IA via OpenRouter, com modelo gratuito para uso pessoal

- **Status:** Aceito
- **Data:** 2026-11-10
- **Substitui:** a escolha de provedor e de modelo do [ADR 0015](0015-ia-sdk-modelo-saida-estruturada.md)
  (o resto do 0015 continua valendo: saída estruturada, validação local, retry, `ai_calls`)

## Contexto
O Rota Dev é, por enquanto, de uso pessoal, e o objetivo é **não ter custo com IA**. O ADR 0015 escolheu o
Claude Opus 5.5, que é pago. O OpenRouter dá acesso a vários modelos pela mesma API, inclusive gratuitos.

Pesquisa feita em 10/11/2026 na API de modelos do OpenRouter: dos 15 modelos gratuitos, poucos aceitam saída
estruturada com JSON Schema e têm qualidade para a tarefa. O melhor candidato foi o
`nvidia/nemotron-3.5-super-vl:free` (120B parâmetros, 12B ativos, contexto de 1M, saída estruturada).

## Decisão
- Novo `OpenRouterLlmGateway`, escolhido por `rota.ai.provider=openrouter` (o padrão). O
  `AnthropicLlmGateway` continua disponível com `rota.ai.provider=anthropic`.
- Modelo padrão `nvidia/nemotron-3.5-super-vl:free`, trocável por `ROTA_AI_OPENROUTER_MODEL`.
- `response_format` com o schema e `strict: true`, mais `provider.require_parameters: true` para só usar
  provedores que respeitam o parâmetro. A validação local do `CareerAiService` continua.
- Modelos `:free` têm custo zero em `ai_calls`.
- Chave ausente ou recusada (400, 401, 402, 403) falha na hora, sem retry; 429 e 5xx seguem o retry do job.

## Alternativas descartadas
- **Modelo local (Ollama):** de graça e privado, mas o Caio prefere não rodar nada local.
- **Só regras, sem LLM:** grátis e previsível, mas sem a leitura de texto livre (currículo, LinkedIn).
- **Claude (ADR 0015):** melhor qualidade, mas pago.

## Consequências
- **Privacidade:** endpoints gratuitos podem registrar os prompts (a NVIDIA diz registrar o uso do endpoint
  gratuito para melhorar os produtos dela). Aceitável para uso pessoal, com os dados do próprio Caio.
  **Com usuários reais isto precisa mudar**: modelo pago com provedores que não guardam dados
  (`provider.data_collection: "deny"`), ou o Claude. A versão paga do mesmo Nemotron custa cerca de
  US$ 0,004 por geração.
- **Limites do gratuito:** 20 pedidos por minuto e 50 por dia, suficientes para uso pessoal.
- **Qualidade:** abaixo do Claude e ainda não medida nos nossos prompts. Se o JSON vier inválido com
  frequência ou o texto ficar fraco, trocar de modelo é configuração.
