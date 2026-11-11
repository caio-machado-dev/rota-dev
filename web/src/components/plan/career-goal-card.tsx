import { Pencil } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Goal } from "@/lib/api/client";

import { LEVELS, TRACKS } from "./goal-form";

const label = (list: readonly (readonly [string, string])[], value: string) => list.find(([v]) => v === value)?.[1] ?? value;

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" });

/** Resumo compacto do objetivo salvo: o formulário vira isto depois de salvar. */
export function CareerGoalCard({ goal, onEdit }: { goal: Goal; onEdit: () => void }) {
  return (
    <Card>
      <CardHeader>
        <div>
          <CardDescription>Cargo-alvo</CardDescription>
          <CardTitle className="mt-1 text-h2">{goal.targetRole}</CardTitle>
        </div>
        <Button variant="outline" size="sm" onClick={onEdit}>
          <Pencil aria-hidden /> Editar
        </Button>
      </CardHeader>
      <div className="flex flex-wrap gap-2">
        <Badge tone="brand">{label(LEVELS, goal.targetLevel)}</Badge>
        <Badge>{label(TRACKS, goal.track)}</Badge>
        {goal.wantsCertifications ? <Badge tone="info">Com certificações</Badge> : null}
      </div>
      <dl className="mt-5 grid gap-x-8 gap-y-3 text-small sm:grid-cols-3">
        <div>
          <dt className="text-muted-foreground">Prazo</dt>
          <dd className="num mt-0.5 text-body">{dateFormat.format(new Date(`${goal.deadline}T12:00:00`))}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Dedicação</dt>
          <dd className="num mt-0.5 text-body">{goal.hoursPerWeek}h por semana</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Stack principal</dt>
          <dd className="mt-0.5 text-body">{goal.primaryStack || "Não informada"}</dd>
        </div>
      </dl>
      {goal.notes ? <p className="mt-4 border-t border-border pt-3 text-small text-muted-foreground">{goal.notes}</p> : null}
    </Card>
  );
}
