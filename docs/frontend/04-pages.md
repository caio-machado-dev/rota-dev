# Páginas

## Agora (`/`)
- **Objetivo:** o que fazer hoje e como está a semana. Acesso diário.
- **Hierarquia:** cabeçalho (saudação no fuso do usuário, resumo da semana, horas × meta, ritmo, "Registrar sessão") → coluna principal
  (semana do plano, protocolo anti vibe coding, sessões de hoje) → painel lateral (prontidão com composição, próximo portão, check-in, reajustes).
- **Componentes:** `NowHeader`, `WeekPlan`, `AntiVibe`, `SessionList`, `ReadinessCard`, `SidePanel`, `SessionSheet` (global), `Sparkline`.
- **Dados:** `GET /api/me`, `/api/now`, `/api/readiness`, `/api/plan/current`, `/api/milestones`, `/api/checkin`, `/api/adjustments`;
  escreve em `POST /api/tracking/sessions`, `DELETE /api/tracking/sessions/{id}`, `PUT /api/tracking/review-habit`.
- **Registro de sessão** (botão, tecla `n`, paleta, botão flutuante no celular): manual com presets 15/30/45/60/90 ou cronômetro; data opcional;
  campos preservados em caso de erro. Cada tarefa do plano abre o registro já preenchido.
- **Estados:** esqueleto com a forma da tela; erro com "tentar de novo"; sem plano (vazio com link); sem sessões hoje (vazio).
- **Mobile:** uma coluna, navegação inferior, botão flutuante, sheet de baixo para cima.
- **Limitações do backend (não simuladas):**
  - tarefa do plano não tem "concluída" na API: o botão "Registrar" cria a sessão correspondente;
  - sem streak/histórico do protocolo (a API devolve só o dia atual) e sem contribuições semanais do GitHub;
  - "ritmo" e a frase da semana são calculados no cliente a partir de minutos e meta (`lib/format.ts`).
