"use client";

import { CircleCheck, Lock } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useSWRConfig } from "swr";

import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input, Textarea } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { ApiError, submitCheckin, type Checkin } from "@/lib/api/client";
import { keys, useAdjustments, useCheckinHistory, useCheckinStatus } from "@/lib/api/hooks";
import { duration } from "@/lib/format";
import { cn } from "@/lib/utils";

const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });
const week = (iso: string) => shortDate.format(new Date(`${iso}T12:00:00`));

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-small text-muted-foreground">{label}</dt>
      <dd className="num mt-0.5 text-h2 font-semibold">{value}</dd>
    </div>
  );
}

function CheckinForm({ weekIso, onDone }: { weekIso: string; onDone: () => Promise<void> }) {
  const [commits, setCommits] = useState(0);
  const [energy, setEnergy] = useState(3);
  const [blockers, setBlockers] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      await submitCheckin({ commits, energy, blockers, notes });
      await onDone();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui salvar o check-in. Seus dados continuam aqui; tente de novo.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="grid grid-cols-1 gap-5 md:grid-cols-2" aria-label={`Check-in da semana de ${week(weekIso)}`}>
      <Field label="Commits na semana" hint="A API não lê seus commits ainda: informe o número.">
        <Input type="number" min={0} max={1000} required value={commits} onChange={(e) => setCommits(Number(e.target.value))} className="num w-32" />
      </Field>
      <fieldset>
        <legend className="mb-1.5 text-small font-medium">Energia (1 a 5)</legend>
        <div role="radiogroup" aria-label="Energia" className="flex gap-2">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              role="radio"
              aria-checked={energy === n}
              onClick={() => setEnergy(n)}
              className={cn("num size-10 rounded-control border border-input text-body transition-colors hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring", energy === n && "border-brand bg-brand/10 font-semibold")}
            >
              {n}
            </button>
          ))}
        </div>
      </fieldset>
      <Field label="O que travou" className="md:col-span-2">
        <Input value={blockers} onChange={(e) => setBlockers(e.target.value)} maxLength={2000} placeholder="Bloqueios da semana, se houve" />
      </Field>
      <Field label="Observações" className="md:col-span-2">
        <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={2000} rows={3} />
      </Field>
      <div className="flex flex-wrap items-center gap-3 md:col-span-2">
        <Button type="submit" disabled={pending}>
          {pending ? "Fechando…" : "Fechar a semana"}
        </Button>
        {error ? (
          <span role="alert" className="text-small text-destructive">
            {error}
          </span>
        ) : null}
      </div>
    </form>
  );
}

function History({ items }: { items: Checkin[] }) {
  if (items.length === 0) return <EmptyState title="Nenhum check-in ainda" description="O histórico aparece depois do primeiro check-in." className="py-6" />;
  return (
    <ul className="divide-y divide-border">
      {[...items]
        .sort((a, b) => b.weekStart.localeCompare(a.weekStart))
        .map((c) => (
          <li key={c.weekStart} className="grid gap-x-6 gap-y-1 py-3 first:pt-0 last:pb-0 sm:grid-cols-[7rem_1fr_auto]">
            <span className="num text-body font-medium">Semana de {week(c.weekStart)}</span>
            <span className="text-small text-muted-foreground">
              {duration(c.minutesLogged)} · {c.reviewDays} dias de revisão · {c.commits} commits{c.blockers ? ` · Bloqueios: ${c.blockers}` : ""}
            </span>
            <span className="num text-small text-muted-foreground">energia {c.energy}/5</span>
          </li>
        ))}
    </ul>
  );
}

/** Check-in semanal: fechado (fora da janela), aberto (formulário com o resumo automático) ou concluído (resultado e reajustes). */
export function CheckinSection() {
  const { data: status, error, isLoading, mutate: reload } = useCheckinStatus();
  const { data: history } = useCheckinHistory();
  const { data: adjustments } = useAdjustments();
  const { mutate } = useSWRConfig();
  const toast = useToast();

  if (isLoading) return <Skeleton className="h-72" />;
  if (error || !status) return <ErrorState message="Não consegui carregar o check-in." onRetry={() => void reload()} />;

  const proposed = (adjustments ?? []).filter((a) => a.status === "PROPOSED");
  const state = status.done ? "concluido" : status.openWeek ? "aberto" : "fechado";

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>{state === "fechado" ? "Check-in fechado" : `Semana de ${week(status.openWeek ?? status.done!.weekStart)}`}</CardTitle>
            <CardDescription>
              {state === "fechado" && "O check-in abre no sábado e fica aberto até terça."}
              {state === "aberto" && "Aberto: feche a semana para atualizar a prontidão."}
              {state === "concluido" && "Concluído. Boa semana."}
            </CardDescription>
          </div>
          <Badge tone={state === "aberto" ? "brand" : state === "concluido" ? "success" : "neutral"}>
            {state === "fechado" ? <Lock aria-hidden /> : state === "concluido" ? <CircleCheck aria-hidden /> : null}
            {state === "fechado" ? "Fechado" : state === "aberto" ? "Aberto" : "Concluído"}
          </Badge>
        </CardHeader>

        <dl className="mb-6 grid grid-cols-2 gap-6 sm:grid-cols-3">
          <Stat label="Minutos registrados" value={duration(status.minutesLogged)} />
          <Stat label="Dias de revisão" value={String(status.reviewDays)} />
          {status.done ? <Stat label="Commits" value={String(status.done.commits)} /> : null}
        </dl>

        {state === "aberto" ? (
          <CheckinForm
            weekIso={status.openWeek!}
            onDone={async () => {
              await Promise.all([mutate(keys.checkin), mutate("/api/checkin/history"), mutate(keys.readiness), mutate(keys.adjustments)]);
              toast.success("Check-in concluído.");
            }}
          />
        ) : null}

        {state === "concluido" && status.done ? (
          <div className="space-y-2 text-body">
            <p>
              Energia <span className="num font-medium">{status.done.energy}/5</span>.
              {status.done.blockers ? <span className="text-muted-foreground"> Bloqueios: {status.done.blockers}</span> : null}
            </p>
            {status.done.notes ? <p className="text-muted-foreground">{status.done.notes}</p> : null}
            {proposed.length > 0 ? (
              <p className="mt-4 rounded-control border border-brand/30 bg-brand/10 p-3 text-small">
                O check-in gerou {proposed.length} {proposed.length === 1 ? "sugestão de reajuste" : "sugestões de reajuste"} do plano.{" "}
                <Link href="/progresso/reajustes" className="font-medium text-brand underline underline-offset-2">
                  Revisar e decidir
                </Link>
              </p>
            ) : (
              <p className="mt-4 text-small text-muted-foreground">Nenhum reajuste sugerido: o plano segue como está.</p>
            )}
          </div>
        ) : null}
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Histórico</CardTitle>
            <CardDescription>Check-ins anteriores.</CardDescription>
          </div>
        </CardHeader>
        {history ? <History items={history} /> : <Skeleton className="h-24" />}
      </Card>
    </div>
  );
}
