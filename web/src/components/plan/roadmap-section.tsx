"use client";

import { Flag, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";
import { useSWRConfig } from "swr";

import { EvidenceChip } from "@/components/evidence/evidence-chip";
import { JobBanner } from "@/components/system/job-banner";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiError, requestPlan, type PlanVersion } from "@/lib/api/client";
import { keys, useGoal, useLatestDiagnosis, useLatestPlan, useMilestones, useNow, useProfile } from "@/lib/api/hooks";
import { duration, KIND_LABEL } from "@/lib/format";
import { cn } from "@/lib/utils";

type Content = NonNullable<PlanVersion["content"]>;

const RESOURCE_KIND: Record<string, string> = { COURSE: "Cursos", BOOK: "Livros", DOC: "Documentação", CERT: "Certificações", ARTICLE: "Artigos" };

function AnchorProject({ anchor }: { anchor: Content["anchorProject"] }) {
  const { data: profile } = useProfile();
  // As evidências do projeto-âncora são ids de evidências do perfil: resolvemos os títulos aqui.
  const byId = useMemo(() => new Map((profile?.snapshots ?? []).flatMap((s) => s.evidences).map((e) => [e.id, e])), [profile]);
  return (
    <Card aria-labelledby="ancora" className="border-brand/30">
      <p className="text-micro font-medium uppercase tracking-wider text-brand">Projeto-âncora</p>
      <h2 id="ancora" className="mt-1 text-h2 font-semibold tracking-tight">
        {anchor.title}
      </h2>
      <p className="mt-2 max-w-3xl text-body text-muted-foreground">{anchor.description}</p>
      {anchor.deliverables.length > 0 ? (
        <>
          <p className="mt-4 text-small font-medium">Entregáveis</p>
          <ul className="mt-2 grid gap-1.5 text-body sm:grid-cols-2">
            {anchor.deliverables.map((d) => (
              <li key={d} className="flex gap-2">
                <span aria-hidden className="mt-2 size-1.5 shrink-0 rounded-full bg-brand" />
                {d}
              </li>
            ))}
          </ul>
        </>
      ) : null}
      {anchor.evidenceIds.length > 0 ? (
        <div className="mt-4">
          <p className="mb-2 text-small font-medium">Evidências relacionadas</p>
          <ul className="flex flex-wrap gap-2">
            {anchor.evidenceIds.map((id) => (
              <li key={id}>
                <EvidenceChip title={byId.get(id)?.title} url={byId.get(id)?.url} />
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Card>
  );
}

function QuarterView({ content, achieved }: { content: Content; achieved: Map<string, string> }) {
  return (
    <ol className="space-y-4">
      {content.quarters.map((q) => (
        <li key={q.index}>
          <Card>
            <CardHeader>
              <div>
                <CardDescription>Trimestre {q.index}</CardDescription>
                <CardTitle className="mt-0.5">{q.title}</CardTitle>
              </div>
              <Badge>{q.weeks.length} semanas</Badge>
            </CardHeader>
            <p className="text-body text-muted-foreground">{q.focus}</p>
            {q.milestones.map((m) => {
              const date = achieved.get(m.title);
              return (
                <div key={m.title} className="mt-4 rounded-control border border-border p-3">
                  <p className="flex flex-wrap items-center gap-2 text-body font-medium">
                    <Flag className="size-4 text-brand" strokeWidth={1.5} aria-hidden />
                    Portão: {m.title}
                    {date ? <Badge tone="success">Conquistado em {date}</Badge> : <Badge>Pendente</Badge>}
                  </p>
                  <ul className="mt-2 list-disc space-y-1 pl-8 text-small text-muted-foreground">
                    {m.criteria.map((c) => (
                      <li key={c}>{c}</li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </Card>
        </li>
      ))}
    </ol>
  );
}

function WeekView({ content, currentWeek }: { content: Content; currentWeek: number | undefined }) {
  return (
    <div className="space-y-8">
      {content.quarters.map((q) => (
        <section key={q.index} aria-label={`Semanas do trimestre ${q.index}`}>
          <h3 className="mb-3 text-small font-medium text-muted-foreground">
            Trimestre {q.index} · {q.title}
          </h3>
          {q.weeks.length === 0 ? <p className="text-small text-muted-foreground">As semanas deste trimestre são detalhadas quando ele chega.</p> : null}
          <ol className="space-y-3">
            {q.weeks.map((w) => {
              const current = w.index === currentWeek;
              const past = currentWeek !== undefined && w.index < currentWeek;
              return (
                <li key={w.index} aria-current={current ? "step" : undefined} className={cn("rounded-card border bg-card p-4", current ? "border-brand/60 shadow-surface" : "border-border", past && "opacity-70")}>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-body font-medium">
                      Semana {w.index} · {w.theme}
                    </p>
                    <span className="flex items-center gap-2">
                      {current ? <Badge tone="brand">Semana atual</Badge> : past ? <Badge>Concluída</Badge> : null}
                      <span className="num text-small text-muted-foreground">{duration(w.totalMinutes)}</span>
                    </span>
                  </div>
                  <ul className="mt-3 space-y-1.5">
                    {w.tasks.map((t) => (
                      <li key={t.title} className="flex items-center gap-3 text-body">
                        <Badge>{KIND_LABEL[t.kind] ?? t.kind}</Badge>
                        <span className="min-w-0 flex-1">{t.title}</span>
                        <span className="num text-small text-muted-foreground">{duration(t.minutes)}</span>
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
          </ol>
        </section>
      ))}
    </div>
  );
}

function Resources({ resources }: { resources: Content["resources"] }) {
  const groups = Object.entries(Object.groupBy(resources, (r) => r.kind));
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Recursos de aprendizado</CardTitle>
          <CardDescription>Cada item está no plano por um motivo.</CardDescription>
        </div>
      </CardHeader>
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {groups.map(([kind, items]) => (
          <section key={kind}>
            <h3 className="mb-2 text-small font-medium text-muted-foreground">{RESOURCE_KIND[kind] ?? kind}</h3>
            <ul className="space-y-3">
              {items?.map((r) => (
                <li key={r.title}>
                  <p className="text-body font-medium">{r.title}</p>
                  <p className="text-small text-muted-foreground">Por que: {r.reason}</p>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </Card>
  );
}

/** Aba "Roadmap": projeto-âncora, visão trimestral (portões) e semanal (semana atual em destaque) e recursos. */
export function RoadmapSection() {
  const { data: plan, error, isLoading, mutate: reload } = useLatestPlan();
  const { data: goal } = useGoal();
  const { data: diagnosis } = useLatestDiagnosis();
  const { data: now } = useNow();
  const { data: milestones } = useMilestones();
  const { mutate } = useSWRConfig();
  const [requestError, setRequestError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const achieved = useMemo(
    () => new Map((milestones ?? []).map((m) => [m.milestone, new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(new Date(`${m.achievedOn}T12:00:00`))])),
    [milestones],
  );

  async function generate() {
    setPending(true);
    setRequestError(null);
    try {
      await requestPlan();
      await mutate(keys.planLatest);
    } catch (e) {
      setRequestError(e instanceof ApiError ? e.message : "Não consegui pedir o plano.");
    } finally {
      setPending(false);
    }
  }

  if (isLoading) return <Skeleton className="h-96" />;
  if (error) return <ErrorState message="Não consegui carregar o plano." onRetry={() => void reload()} />;

  const running = plan?.status === "PENDING";
  const content = plan?.status === "READY" ? plan.content : undefined;
  const hasDiagnosis = diagnosis?.status === "READY";

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={() => void generate()} disabled={pending || running || !goal || !hasDiagnosis}>
          <Sparkles aria-hidden /> {running ? "Montando…" : plan ? "Gerar nova versão" : "Gerar plano"}
        </Button>
        {plan ? <Badge>v{plan.version}</Badge> : null}
        {!goal || !hasDiagnosis ? <span className="text-small text-muted-foreground">O plano precisa de objetivo e diagnóstico prontos.</span> : null}
        {requestError ? (
          <span role="alert" className="text-small text-destructive">
            {requestError}
          </span>
        ) : null}
      </div>

      <JobBanner status={plan?.status} runningLabel="Montando o roadmap a partir do diagnóstico." failedMessage="Não foi possível montar o plano. Tente de novo em alguns minutos." onRetry={() => void generate()} />

      {content ? (
        <>
          <p className="max-w-3xl text-body leading-relaxed">{content.summary}</p>
          <AnchorProject anchor={content.anchorProject} />
          <Tabs defaultValue="trimestres">
            <TabsList aria-label="Visão do roadmap">
              <TabsTrigger value="trimestres">Trimestres</TabsTrigger>
              <TabsTrigger value="semanas">Semanas</TabsTrigger>
            </TabsList>
            <TabsContent value="trimestres">
              <QuarterView content={content} achieved={achieved} />
            </TabsContent>
            <TabsContent value="semanas">
              <WeekView content={content} currentWeek={now?.planWeek?.index} />
            </TabsContent>
          </Tabs>
          {content.resources.length > 0 ? <Resources resources={content.resources} /> : null}
        </>
      ) : !plan ? (
        <Card>
          <EmptyState title="Ainda não há um plano" description="Com objetivo e diagnóstico prontos, o roadmap é montado para caber nas suas horas semanais." />
        </Card>
      ) : null}
    </div>
  );
}
