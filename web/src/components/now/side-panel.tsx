"use client";

import { ArrowRight, Flag } from "lucide-react";
import Link from "next/link";

import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Adjustment, CheckinStatus, MilestoneAchievement, PlanVersion } from "@/lib/api/client";

/** O primeiro portão do plano que ainda não foi conquistado (compara pelo título). */
export function nextGate(plan: PlanVersion | null | undefined, achieved: MilestoneAchievement[] | undefined) {
  if (plan?.status !== "READY" || !plan.content) return null;
  const done = new Set((achieved ?? []).map((a) => a.milestone));
  for (const quarter of plan.content.quarters) {
    const gate = quarter.milestones.find((m) => !done.has(m.title));
    if (gate) return { title: gate.title, quarter: quarter.title, criteria: gate.criteria };
  }
  return null;
}

function SideLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link href={href} className="mt-3 inline-flex items-center gap-1 text-small text-brand hover:underline">
      {children} <ArrowRight className="size-3.5" strokeWidth={1.5} aria-hidden />
    </Link>
  );
}

/** Próximo portão, check-in da semana e reajustes à espera de decisão. */
export function SidePanel({
  plan,
  achievements,
  checkin,
  adjustments,
}: {
  plan: PlanVersion | null | undefined;
  achievements: MilestoneAchievement[] | undefined;
  checkin: CheckinStatus | undefined;
  adjustments: Adjustment[] | undefined;
}) {
  const gate = nextGate(plan, achievements);
  const proposed = (adjustments ?? []).filter((a) => a.status === "PROPOSED");

  return (
    <>
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Próximo portão</CardTitle>
            <CardDescription>{gate ? gate.quarter : "Marco que prova sua evolução"}</CardDescription>
          </div>
          <Flag className="size-4 text-muted-foreground" strokeWidth={1.5} aria-hidden />
        </CardHeader>
        {gate ? (
          <>
            <p className="text-body font-medium">{gate.title}</p>
            <ul className="mt-2 list-disc space-y-1 pl-4 text-small text-muted-foreground">
              {gate.criteria.slice(0, 3).map((c) => (
                <li key={c}>{c}</li>
              ))}
            </ul>
          </>
        ) : (
          <p className="text-small text-muted-foreground">Sem portões pendentes. Eles aparecem quando o plano estiver pronto.</p>
        )}
        <SideLink href="/progresso/portoes">Ver portões</SideLink>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Check-in semanal</CardTitle>
            <CardDescription>{checkin?.done ? "Concluído nesta semana" : checkin?.openWeek ? "Aberto para esta semana" : "Fechado: abre no sábado"}</CardDescription>
          </div>
        </CardHeader>
        <p className="num text-small text-muted-foreground">
          {checkin ? `${checkin.minutesLogged} min registrados · ${checkin.reviewDays} dias de revisão` : "—"}
        </p>
        <SideLink href="/progresso/checkin">{checkin?.openWeek && !checkin.done ? "Fazer o check-in" : "Ver check-ins"}</SideLink>
      </Card>

      {proposed.length > 0 ? (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Reajustes pendentes</CardTitle>
              <CardDescription>
                {proposed.length} {proposed.length === 1 ? "proposta aguarda" : "propostas aguardam"} sua decisão
              </CardDescription>
            </div>
          </CardHeader>
          <p className="text-small text-muted-foreground">{proposed[0].reason}</p>
          <SideLink href="/progresso/reajustes">Decidir</SideLink>
        </Card>
      ) : null}
    </>
  );
}
