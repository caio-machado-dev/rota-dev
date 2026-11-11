"use client";

import { Plus } from "lucide-react";

import { useSessionUi } from "@/components/sessions/session-context";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { duration, firstName, greeting, longDate, pace, PACE_LABEL, type Pace } from "@/lib/format";
import type { Me, Now } from "@/lib/api/client";

const WEEKDAY = ["", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];
const PACE_TONE: Record<Exclude<Pace, "none">, "success" | "brand" | "warning"> = { ahead: "success", "on-track": "brand", behind: "warning" };

/** Uma frase honesta sobre a semana, derivada só dos números (sem texto motivacional vazio). */
export function weekSummary(now: Now): string {
  const budget = now.weeklyBudgetMinutes;
  if (!budget) return "Defina suas horas semanais no objetivo para acompanhar o ritmo.";
  if (now.minutesThisWeek >= budget) return "Meta da semana batida. O que passar daqui é bônus.";
  if (now.minutesThisWeek === 0) return "A semana ainda não tem sessões. Uma de 30 minutos já põe o ritmo em marcha.";
  return `Faltam ${duration(budget - now.minutesThisWeek)} para a meta da semana.`;
}

/** Saudação, resumo da semana e a ação principal do dia. */
export function NowHeader({ me, now }: { me: Me; now: Now }) {
  const { openSession } = useSessionUi();
  const budget = now.weeklyBudgetMinutes;
  const ritmo = pace(now.minutesThisWeek, budget, now.weekday);

  return (
    <header className="flex flex-wrap items-end justify-between gap-x-8 gap-y-5">
      <div className="min-w-0">
        <p className="text-small text-muted-foreground">
          {WEEKDAY[now.weekday]}, {longDate(now.today)}
        </p>
        <h1 className="mt-1 text-h1 font-semibold tracking-tight">
          {greeting(new Date(), me.timeZone)}, {firstName(me)}.
        </h1>
        <p className="mt-1 text-body text-muted-foreground">{weekSummary(now)}</p>
      </div>

      <div className="flex w-full flex-col gap-3 sm:w-auto sm:min-w-72">
        <div className="flex items-baseline justify-between gap-3">
          <p>
            <span className="num text-h2 font-semibold">{duration(now.minutesThisWeek)}</span>
            <span className="text-small text-muted-foreground">{budget ? ` de ${duration(budget)} nesta semana` : " nesta semana"}</span>
          </p>
          {ritmo !== "none" ? <Badge tone={PACE_TONE[ritmo]}>{PACE_LABEL[ritmo]}</Badge> : null}
        </div>
        {budget ? <Progress label="Horas da semana" value={now.minutesThisWeek} max={budget} /> : null}
        <Button onClick={() => openSession()} className="hidden self-start md:inline-flex">
          <Plus aria-hidden /> Registrar sessão <kbd className="num ml-1 rounded-[4px] border border-current/30 px-1 text-micro opacity-70">n</kbd>
        </Button>
      </div>
    </header>
  );
}
