# Glossário do Rota Dev

Termos do domínio, usados igual no código, na UI e nas conversas.

| Termo | Significado | No código |
|---|---|---|
| **Fonte** | Origem de dados do usuário: GitHub, LinkedIn, currículo ou manual | `ConnectedSource`, `SourceType` |
| **Snapshot de perfil** | Fotografia normalizada do perfil num momento, com origem e confiança de cada dado. Reprocessar gera um novo, e o histórico é preservado | `ProfileSnapshot` |
| **Evidência** | Fato verificável que sustenta uma conclusão (ex.: "repo X tem testes de integração e CI") | `Evidence` |
| **Diagnóstico** | Nível estimado por eixo, radar e lacunas, sempre citando evidências | `Diagnosis` |
| **Lacuna** | Distância entre a nota atual e a nota exigida pelo cargo-alvo numa competência | `Gap` |
| **Objetivo de carreira** | Cargo-alvo, prazo, horas por semana e preferências | `CareerGoal` |
| **Plano** | Roadmap versionado em trimestres e semanas | `CareerPlan`, `PlanVersion` |
| **Portão** | Marco verificável que prova evolução (ex.: "API Spring com testes e CI publicada") | `Milestone` |
| **Projeto-âncora** | Projeto prático central do plano, derivado do histórico real do usuário | `AnchorProject` |
| **Semana padrão** | Distribuição recorrente das atividades nos dias da semana | `WeeklyTemplate` |
| **Sessão de estudo** | Registro de tipo, dia, minutos, tema e uma linha de aprendizado | `StudySession` |
| **Check-in** | Revisão semanal (domingo) de horas, revisões de diff, commits e bloqueios | `WeeklyCheckin` |
| **Índice de prontidão** | Nota de 0 a 100 composta por estudo, habilidades, portões e hábito | `ReadinessScore` |
| **Protocolo anti vibe coding** | Rotina diária de revisar o código gerado por IA: ler o diff, explicar, testar, anotar | `ReviewHabit` |
| **Reajuste** | Proposta de mudança no plano a partir do check-in, aprovada ou recusada pelo usuário | `PlanAdjustment` |
