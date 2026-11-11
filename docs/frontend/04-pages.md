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

## Plano (`/plano`, `/plano/diagnostico`, `/plano/roadmap`, `/plano/versoes`)
Layout comum com `PageHeader` + `SectionNav` (navegação por rota; o breadcrumb mostra a subseção).
- **Objetivo:** `GoalSection` → formulário (`GoalForm`, campos controlados, erros da API sem perder dados) ou resumo compacto (`CareerGoalCard`).
  Dados: `GET/PUT /api/goal`.
- **Diagnóstico:** `DiagnosisSection` → `DiagnosisHeader` (nível, **compatibilidade** = soma de min(nota, exigido) ÷ soma do exigido, cálculo explicado na tela),
  `SkillRadar` (atual × exigido; clique no eixo seleciona), lista de competências, `SkillDetail` (nota, exigida, falta, justificativa, evidências,
  "Não verificado" quando não há prova), `SkillGapChart`. Job: `POST /api/diagnosis` + polling de 3 s enquanto `PENDING` (`JobBanner`; sem etapas inventadas).
- **Roadmap:** projeto-âncora (evidências resolvidas pelos ids no perfil), abas Trimestres (portões com conquistado/pendente) e Semanas
  (semana atual destacada via `/api/now`), recursos agrupados com o motivo. Geração: `POST /api/plan`.
- **Versões:** histórico estilo `git log` (versão, id curto, motivo, data, status) e **comparação** entre duas versões (`lib/domain/plan-diff.ts`,
  calculada no cliente com `GET /api/plan/versions/{id}`).
- **Limitações (não simuladas):** sem restaurar versão, sem autor da versão (a API não informa), sem contestar nota, sem checklist persistido do
  projeto-âncora, a API só marca como dependente a geração do plano de objetivo + diagnóstico prontos.

## Progresso (`/progresso`, `/progresso/checkin`, `/progresso/portoes`, `/progresso/reajustes`)
- **Visão geral:** `ReadinessGauge` (radial, número em texto), `ReadinessComposition` (peso e pontos de cada componente: 40/25/20/15), variação desde o último ponto,
  `ReadinessTrend` (índice + componentes, ativáveis), `ConsistencyHeatmap` (12 semanas, de `GET /api/tracking/sessions?from&to`), `SessionMix`,
  `WeeklyHoursChart` (com a meta), `GoalComparison` (atual × exigido do diagnóstico). Cada gráfico tem tooltip, estado vazio e tabela oculta para leitores de tela.
  Os números do heatmap, horas e mix são calculados no cliente a partir das sessões (`lib/domain/consistency.ts`, testado).
- **Check-in:** estados Fechado (abre no sábado, fecha na terça), Aberto (resumo automático + commits, energia 1–5, bloqueios, notas) e Concluído
  (resultado, link para reajustes sugeridos); histórico de `GET /api/checkin/history`. Commits são digitados: a API não os lê do GitHub.
- **Portões:** trajeto com conquistados (data, versão, evidência), próximo marco e pendentes; "Registrar conquista" pede a URL (`POST /api/milestones`).
- **Reajustes:** motivo, semana afetada, status; aceitar pede confirmação explícita (gera nova versão via job), recusar mantém o plano; aceitos mostram
  o antes/depois como diff entre a versão gerada e a anterior. **Não há** "impacto esperado" nem "histórico de decisões" além do status (API).
- **Atalhos/paleta:** `c` e "Abrir check-in" levam a `/progresso/checkin`.

## Perfil (`/perfil`, `/perfil/encontramos`, `/perfil/dados`)
- **Fontes:** GitHub (conta, conexão, última leitura, dados identificados, "Atualizar agora": job com acompanhamento por `lastSyncedAt` até 90 s),
  LinkedIn e Currículo (`FileUploader`: clique ou arraste, validação de formato e 10 MB antes de enviar, lista com formato, tamanho e estado,
  polling enquanto `RECEIVED`), Dados manuais (indisponível na API, sem botão).
- **O que encontramos:** habilidades das leituras mais recentes de cada fonte (confiança, peso, fonte, evidências ao expandir), filtros por fonte/
  confiança/nome (a paleta de comandos abre aqui com `?q=`), comparação das duas últimas leituras de uma fonte (entrou/saiu/mudou de peso).
  **Sem** confirmar/corrigir/remover/adicionar (a API não tem).
- **Meus dados:** conta (nome, login, e-mail, fuso, plano), exportação JSON, privacidade e exclusão com diálogo + digitar o login.
- A paleta (⌘K) agora busca habilidades e evidências do perfil.
