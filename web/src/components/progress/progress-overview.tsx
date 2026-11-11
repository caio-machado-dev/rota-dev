"use client";

import { useMemo } from "react";

import { ConsistencyHeatmap } from "@/components/charts/consistency-heatmap";
import { GoalComparison } from "@/components/charts/goal-comparison";
import { ReadinessGauge } from "@/components/charts/readiness-gauge";
import { ReadinessTrend } from "@/components/charts/readiness-trend";
import { SessionMix } from "@/components/charts/session-mix";
import { WeeklyHoursChart } from "@/components/charts/weekly-hours-chart";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useLatestDiagnosis, useNow, useReadiness, useSessions } from "@/lib/api/hooks";
import { addDays, heatmap, minutesByKind, weeklyMinutes } from "@/lib/domain/consistency";
import { duration, readinessDelta } from "@/lib/format";

import { ReadinessComposition } from "./readiness-composition";

const WEEKS = 12;

/** Visão geral: prontidão e sua composição, evolução, consistência, horas, mix de sessões e distância até o cargo-alvo. */
export function ProgressOverview() {
  const { data: readiness, error, mutate } = useReadiness();
  const { data: now } = useNow();
  const { data: diagnosis } = useLatestDiagnosis();
  const today = now?.today ?? null;
  const from = today ? addDays(today, -(WEEKS * 7 - 1)) : null;
  const { data: sessions } = useSessions(from, today);

  const derived = useMemo(() => {
    if (!sessions || !today) return null;
    return { grid: heatmap(sessions, today, WEEKS), weeks: weeklyMinutes(sessions, today, WEEKS), mix: minutesByKind(sessions) };
  }, [sessions, today]);

  if (error) return <ErrorState message="Não consegui carregar o progresso." onRetry={() => void mutate()} />;
  if (!readiness) return <Skeleton className="h-96" aria-busy="true" />;

  const delta = readinessDelta(readiness.history);
  const total = sessions?.reduce((sum, s) => sum + s.minutes, 0) ?? 0;

  return (
    <div className="space-y-6">
      <Card>
        <div className="grid grid-cols-1 items-center gap-8 md:grid-cols-[auto_1fr]">
          <div className="flex flex-col items-center gap-3">
            <ReadinessGauge score={readiness.current.score} />
            {delta !== null ? (
              <Badge tone={delta > 0 ? "success" : delta < 0 ? "warning" : "neutral"}>
                {delta > 0 ? "+" : ""}
                {delta} pts desde o último ponto
              </Badge>
            ) : (
              <span className="text-micro text-muted-foreground">A variação aparece após o segundo check-in.</span>
            )}
          </div>
          <div>
            <h2 className="text-h3 font-semibold">Como o índice é calculado</h2>
            <p className="mt-1 mb-5 text-small text-muted-foreground">Quatro componentes de 0 a 100, com pesos fixos. Nada fica escondido.</p>
            <ReadinessComposition current={readiness.current} />
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Evolução</CardTitle>
            <CardDescription>Um ponto por semana. Ative os componentes para ver o que puxa o índice.</CardDescription>
          </div>
        </CardHeader>
        <ReadinessTrend history={readiness.history} />
      </Card>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Consistência</CardTitle>
              <CardDescription>Últimas {WEEKS} semanas, dia a dia.</CardDescription>
            </div>
          </CardHeader>
          {derived ? <ConsistencyHeatmap grid={derived.grid} /> : <Skeleton className="h-32" />}
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Tipos de sessão</CardTitle>
              <CardDescription>{total > 0 ? `${duration(total)} nas últimas ${WEEKS} semanas` : "Como você divide o tempo"}</CardDescription>
            </div>
          </CardHeader>
          {derived && derived.mix.length > 0 ? (
            <SessionMix mix={derived.mix} />
          ) : (
            <EmptyState title="Você ainda não registrou sessões" description="Registre uma sessão (tecla n) para ver a distribuição." className="py-6" />
          )}
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Horas por semana</CardTitle>
            <CardDescription>{now?.weeklyBudgetMinutes ? `Meta: ${duration(now.weeklyBudgetMinutes)} por semana` : "Defina horas semanais no objetivo para ver a meta."}</CardDescription>
          </div>
        </CardHeader>
        {derived ? <WeeklyHoursChart weeks={derived.weeks} budgetMinutes={now?.weeklyBudgetMinutes} /> : <Skeleton className="h-56" />}
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Atual × cargo-alvo</CardTitle>
            <CardDescription>Distância por competência, do último diagnóstico.</CardDescription>
          </div>
        </CardHeader>
        {diagnosis?.status === "READY" ? (
          <GoalComparison axes={diagnosis.axes} />
        ) : (
          <EmptyState title="Sem diagnóstico pronto" description="Gere o diagnóstico em Plano para comparar suas notas com o que o cargo exige." className="py-6" />
        )}
      </Card>
    </div>
  );
}
