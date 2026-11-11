# Changelog do redesign (2026-11-10)

PRs empilhados (cada um sobre o anterior; nada foi mergeado nem tocou a `main`): #34 auditoria → #35 tokens e tema → #36 logo e base → #37 shell → #38 Agora →
#39 Plano → #40 Progresso → #41 Perfil → #42 onboarding → #43 refinamento → docs finais.

## Adicionado
- **Design system:** escalas de cor, tokens semânticos, tipografia (display a micro), raios, sombras, movimento; tema escuro (padrão) e claro com alternância sem flash;
  marca verde-limão (escuro) / verde fechado (claro) em `brand.ts`; logotipo "R" como trajeto e favicon.
- **Shell:** sidebar compactável, topbar (migalhas, jobs, busca), navegação inferior, paleta de comandos (cmdk), atalhos, guarda de sessão, `/login`, `/configuracoes`.
- **Agora, Plano, Progresso, Perfil** refeitos com subrotas, gráficos próprios, estados completos e diff de versões no cliente.
- **Onboarding** em 4 etapas; registro de sessão global com cronômetro.
- **Camada de dados** com SWR (ADR 0017) e biblioteca de componentes (`03-components.md`).

## Alterado
- `cn()` agora usa `clsx` + `tailwind-merge` configurado com os nomes do design system (o pacote `cn` apagava `text-small`).
- Páginas movidas para os grupos de rotas `(auth)` e `(dashboard)`; caminhos `/`, `/plano`, `/progresso`, `/perfil` preservados.

## Removido
Painéis antigos (`NowPanel`, `PlanView`, `DiagnosisPanel`, `ProgressView`, `ProfileReview`, `AccountData`, `AdjustmentsList`, `Placeholder`) e o pacote `cn`; a lógica e os cenários de teste foram portados.

## Dependências novas
`swr`, `cmdk`, `clsx`, `tailwind-merge`. Backend: nenhuma mudança.
