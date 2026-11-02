# 0002. Monólito modular com Spring Modulith

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
O produto tem domínios distintos (identidade, fontes, perfil, diagnóstico, plano, acompanhamento, prontidão, IA). Só há um desenvolvedor e um servidor. Precisamos de fronteiras claras sem o custo operacional de vários serviços.

## Decisão
Um único deploy (monólito), dividido em **módulos**. Cada pacote direto abaixo de `dev.rotadev` é um módulo (ex.: `dev.rotadev.identity`). Dentro do módulo:

| Camada | Pode depender de | Contém |
|---|---|---|
| `domain` | nada de framework | Regras de negócio, objetos de valor |
| `application` | `domain` | Casos de uso, `@Transactional` |
| `infrastructure` | `application`, `domain` | Entidades JPA, repositórios, clientes HTTP |
| `web` | `application` | Controllers e DTOs (records) |

O **Spring Modulith** verifica num teste (`ModularityTest`) que um módulo só usa a API pública de outro (os tipos no pacote raiz do módulo) e que não há ciclos. Para comunicação desacoplada entre módulos, use eventos (`ApplicationEventPublisher` + `@ApplicationModuleListener`).

## Alternativas descartadas
- **Microserviços:** rede, deploy e observabilidade multiplicados, sem ganho para o volume atual.
- **ArchUnit puro:** resolve, mas exige escrever cada regra à mão. O Modulith já entende o conceito de módulo e é um projeto oficial do Spring.
- **Camadas globais (`controllers/`, `services/`, ...):** espalha cada funcionalidade pelo projeto e não impede um domínio de mexer nas entranhas de outro.

## Consequências
- O build quebra se alguém atravessar uma fronteira.
- Extrair um módulo para um serviço próprio no futuro fica viável, porque as dependências já são explícitas.
- As regras de camada **dentro** do módulo são convenção, revisada no PR. Se doer, adicionamos ArchUnit para elas.
