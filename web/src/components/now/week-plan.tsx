"use client";

import Link from "next/link";

import { useSessionUi } from "@/components/sessions/session-context";
import { EmptyState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Now } from "@/lib/api/client";
import { duration, KIND_LABEL } from "@/lib/format";

/**
 * Tarefas da semana do plano. A API ainda não marca tarefa como concluída, então cada tarefa
 * oferece "Registrar": abre o registro de sessão já preenchido com tipo, tema e duração.
 */
export function WeekPlan({ now }: { now: Now }) {
  const { openSession } = useSessionUi();
  const week = now.planWeek;

  if (!week) {
    return (
      <Card>
        <EmptyState
          title="Sem plano em vigor para esta semana"
          description="Defina seu objetivo e gere o plano para ver aqui as tarefas de cada semana."
          action={
            <Link href="/plano" className="text-small font-medium text-brand hover:underline">
              Montar meu plano
            </Link>
          }
        />
      </Card>
    );
  }

  return (
    <Card aria-labelledby="semana-plano">
      <CardHeader>
        <div>
          <CardTitle id="semana-plano">
            Semana {week.index} · {week.theme}
          </CardTitle>
          <CardDescription>
            {week.tasks.length} tarefas · {duration(week.totalMinutes)} previstas
          </CardDescription>
        </div>
        <Link href="/plano/roadmap" className="text-small text-brand hover:underline">
          Ver roadmap
        </Link>
      </CardHeader>
      <ul className="divide-y divide-border">
        {week.tasks.map((task) => (
          <li key={task.title} className="flex flex-wrap items-center gap-x-3 gap-y-2 py-3 first:pt-0 last:pb-0">
            <Badge tone="neutral">{KIND_LABEL[task.kind] ?? task.kind}</Badge>
            <span className="min-w-0 flex-1 text-body">{task.title}</span>
            <span className="num text-small text-muted-foreground">{duration(task.minutes)}</span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => openSession({ topic: task.title, minutes: task.minutes, kind: task.kind as "STUDY" | "BUILD" | "REVIEW" | "CERT" })}
              aria-label={`Registrar sessão de ${task.title}`}
            >
              Registrar
            </Button>
          </li>
        ))}
      </ul>
      {now.templateToday.length > 0 ? (
        <p className="mt-4 border-t border-border pt-3 text-small text-muted-foreground">
          Sua semana padrão para hoje: {now.templateToday.map((s) => `${KIND_LABEL[s.kind] ?? s.kind} ${duration(s.minutes)}`).join(" · ")}
        </p>
      ) : null}
    </Card>
  );
}
