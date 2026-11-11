# Interações

## Atalhos
| Tecla | Ação |
|---|---|
| ⌘/Ctrl + K | abre/fecha a paleta de comandos (funciona também dentro de campos) |
| `g` depois `a` / `p` / `r` / `f` | Agora / Plano / Progresso / Perfil (janela de 1 s) |
| `n` | registrar sessão |
| `c` | abrir o check-in |

Não disparam em `input`, `textarea`, `select`, campos editáveis, nem com Ctrl/⌘/Alt (exceto ⌘K). Implementação: `components/shell/shortcuts.ts`.

## Paleta de comandos (cmdk)
Navegar (4 seções + Configurações, com dica de atalho) · Ações (registrar sessão, abrir check-in) · Habilidades e Evidências do perfil (abre
`/perfil/encontramos?q=`) · Tema (escuro, claro, sistema) · Conta (sair). Busca incremental; Esc fecha.

## Registro de sessão
Um único ponto de entrada (`SessionProvider`): botão no Agora, botão flutuante no celular, tecla `n`, paleta e botão "Registrar" em cada tarefa do plano
(abre preenchido). Manual (presets 15/30/45/60/90, data opcional) ou cronômetro (iniciar, pausar, usar o tempo). Sucesso: toast e dados revalidados
(`now`, `readiness`, `checkin`).

## Feedback e confirmações
- Toasts discretos (4,5 s) para sucesso/erro de ações; erros de formulário ficam no próprio formulário.
- **Confirmação explícita:** aceitar reajuste (gera nova versão do plano) e excluir conta (diálogo + digitar o login). Recusar reajuste, apagar sessão e
  salvar são diretos e reversíveis ou de baixo impacto.
- Jobs: botão desabilitado + banner "em andamento" + `JobIndicator` na topbar; a tela se atualiza sozinha.

## Movimento
Fade de página (200 ms), preenchimento da barra de progresso e do gauge (≤ 250 ms), abertura de diálogos (200 ms), skeletons. Nada decorativo;
`prefers-reduced-motion` desliga.

## Fluxos principais
Entrar → (sem objetivo) onboarding → objetivo → diagnóstico → plano → rotina diária em Agora → check-in semanal → reajuste (aceitar/recusar) → nova versão.
