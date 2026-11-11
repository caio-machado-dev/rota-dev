"use client";

import Link from "next/link";

import { ErrorState } from "@/components/system/states";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdjustments, useCheckinStatus, useCurrentPlan, useGoal, useMe, useMilestones, useNow, useReadiness } from "@/lib/api/hooks";

import { AntiVibe } from "./anti-vibe";
import { NowHeader } from "./now-header";
import { ReadinessCard } from "./readiness-card";
import { SessionList } from "./session-list";
import { SidePanel } from "./side-panel";
import { WeekPlan } from "./week-plan";

function NowSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true" aria-label="Carregando o dia">
      <Skeleton className="h-24 w-full" />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="space-y-6">
          <Skeleton className="h-56" />
          <Skeleton className="h-64" />
        </div>
        <div className="space-y-6">
          <Skeleton className="h-80" />
          <Skeleton className="h-36" />
        </div>
      </div>
    </div>
  );
}

/** Tela "Agora": o que fazer hoje, como está a semana e a prontidão. Dados via SWR; a guarda de sessão fica no shell. */
export function NowDashboard() {
  const { data: me } = useMe();
  const { data: now, error, mutate } = useNow();
  const { data: readiness } = useReadiness();
  const { data: plan } = useCurrentPlan();
  const { data: achievements } = useMilestones();
  const { data: checkin } = useCheckinStatus();
  const { data: adjustments } = useAdjustments();
  const { data: goal } = useGoal();

  if (error) return <ErrorState message="Não consegui carregar o seu dia. Tente de novo." onRetry={() => void mutate()} />;
  if (!now || !me) return <NowSkeleton />;

  return (
    <div className="space-y-8">
      {goal === null ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-card border border-brand/30 bg-brand/10 p-4">
          <p className="text-body">Você ainda não definiu um objetivo. Em quatro passos o Rota Dev monta o seu diagnóstico e o plano.</p>
          <Link href="/onboarding" className="text-small font-medium text-brand hover:underline">
            Começar o onboarding
          </Link>
        </div>
      ) : null}
      <NowHeader me={me} now={now} />
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="space-y-6">
          <WeekPlan now={now} />
          {/* A chave refaz os campos quando o registro salvo muda na API. */}
          <AntiVibe key={`${now.today}-${now.habitToday?.stepsDone ?? 0}-${now.habitToday?.note ?? ""}`} now={now} />
          <SessionList now={now} />
        </div>
        <aside className="space-y-6" aria-label="Painel lateral">
          {readiness ? <ReadinessCard readiness={readiness} /> : <Skeleton className="h-80" />}
          <SidePanel plan={plan} achievements={achievements} checkin={checkin} adjustments={adjustments} />
        </aside>
      </div>
    </div>
  );
}
