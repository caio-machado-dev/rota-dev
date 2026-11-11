"use client";

import { Check, Flag } from "lucide-react";
import { useState } from "react";
import { useSWRConfig } from "swr";

import { EvidenceChip } from "@/components/evidence/evidence-chip";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogClose, DialogContent } from "@/components/ui/dialog";
import { Field, Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { achieveMilestone, ApiError } from "@/lib/api/client";
import { keys, useCurrentPlan, useMilestones } from "@/lib/api/hooks";
import { cn } from "@/lib/utils";

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" });

function AchieveDialog({ gate, onClose }: { gate: string | null; onClose: () => void }) {
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const [url, setUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!gate) return;
    setPending(true);
    setError(null);
    try {
      await achieveMilestone(gate, url.trim());
      await Promise.all([mutate(keys.milestones), mutate(keys.readiness)]);
      toast.success(`Portão conquistado: ${gate}.`);
      setUrl("");
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui registrar a conquista. Tente de novo.");
    } finally {
      setPending(false);
    }
  }

  return (
    <Dialog open={gate !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent title="Registrar conquista" description={gate ? `Prove o portão “${gate}” com um link: repositório, PR, deploy ou certificado.` : undefined}>
        <form onSubmit={submit} className="space-y-4">
          <Field label="URL da evidência" hint="Precisa ser um link público (https).">
            <Input type="url" required value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://github.com/você/projeto" />
          </Field>
          {error ? (
            <p role="alert" className="text-small text-destructive">
              {error}
            </p>
          ) : null}
          <div className="flex justify-end gap-2">
            <DialogClose render={<Button type="button" variant="outline" disabled={pending} />}>Cancelar</DialogClose>
            <Button type="submit" disabled={pending || !url.trim()}>
              {pending ? "Registrando…" : "Registrar conquista"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** Portões como marcos num trajeto: conquistados (com data e evidência), o próximo em destaque e os pendentes. */
export function GatesSection() {
  const { data: plan, error, isLoading, mutate: reload } = useCurrentPlan();
  const { data: achievements } = useMilestones();
  const [registering, setRegistering] = useState<string | null>(null);

  if (isLoading) return <Skeleton className="h-72" />;
  if (error) return <ErrorState message="Não consegui carregar os portões." onRetry={() => void reload()} />;

  const gates = plan?.content?.quarters.flatMap((q) => q.milestones.map((m) => ({ ...m, quarter: q.index, quarterTitle: q.title }))) ?? [];
  if (gates.length === 0) {
    return (
      <Card>
        <EmptyState title="Os portões aparecem com o plano" description="Cada trimestre tem um portão: um marco verificável que prova a sua evolução." />
      </Card>
    );
  }

  const done = new Map((achievements ?? []).map((a) => [a.milestone, a]));
  const next = gates.find((g) => !done.has(g.title));
  const doneCount = gates.filter((g) => done.has(g.title)).length;

  return (
    <>
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Trajeto</CardTitle>
            <CardDescription>
              {doneCount} de {gates.length} portões conquistados
            </CardDescription>
          </div>
        </CardHeader>
        <ol>
          {gates.map((g, i) => {
            const a = done.get(g.title);
            const isNext = next?.title === g.title;
            return (
              <li key={g.title} className="relative flex gap-4 pb-8 last:pb-0">
                {i < gates.length - 1 ? <span aria-hidden className={cn("absolute top-8 bottom-0 left-[15px] w-px", a ? "bg-brand" : "bg-border")} /> : null}
                <span
                  aria-hidden
                  className={cn(
                    "relative z-10 grid size-8 shrink-0 place-items-center rounded-full border",
                    a ? "border-brand bg-brand text-brand-foreground" : isNext ? "border-brand bg-card text-brand ring-4 ring-brand/15" : "border-border bg-card text-muted-foreground",
                  )}
                >
                  {a ? <Check className="size-4" strokeWidth={2.5} /> : <Flag className="size-4" strokeWidth={1.5} />}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-body font-medium">{g.title}</h3>
                    {a ? <Badge tone="success">Conquistado</Badge> : isNext ? <Badge tone="brand">Próximo marco</Badge> : <Badge>Pendente</Badge>}
                  </div>
                  <p className="text-small text-muted-foreground">
                    Trimestre {g.quarter} · {g.quarterTitle}
                  </p>
                  <ul className="mt-2 list-disc space-y-0.5 pl-5 text-small text-muted-foreground">
                    {g.criteria.map((c) => (
                      <li key={c}>{c}</li>
                    ))}
                  </ul>
                  {a ? (
                    <p className="mt-3 flex flex-wrap items-center gap-2 text-small text-muted-foreground">
                      Em {dateFormat.format(new Date(`${a.achievedOn}T12:00:00`))} · versão v{a.planVersion}
                      <EvidenceChip title="evidência" url={a.evidenceUrl} />
                    </p>
                  ) : (
                    <Button variant={isNext ? "default" : "outline"} size="sm" className="mt-3" onClick={() => setRegistering(g.title)}>
                      Registrar conquista
                    </Button>
                  )}
                </div>
              </li>
            );
          })}
        </ol>
      </Card>
      <AchieveDialog gate={registering} onClose={() => setRegistering(null)} />
    </>
  );
}
