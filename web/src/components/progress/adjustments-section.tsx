"use client";

import { useState } from "react";
import { useSWRConfig } from "swr";

import { PlanDiff } from "@/components/plan/plan-diff-view";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { type Adjustment, ApiError, decideAdjustment } from "@/lib/api/client";
import { keys, useAdjustments, usePlanVersions } from "@/lib/api/hooks";

const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });
const STATUS: Record<string, { label: string; tone: "brand" | "success" | "neutral" }> = {
  PROPOSED: { label: "Aguardando você", tone: "brand" },
  ACCEPTED: { label: "Aceito", tone: "success" },
  REJECTED: { label: "Recusado", tone: "neutral" },
};

/** "Antes e depois" de um reajuste aceito: compara a versão gerada por ele com a imediatamente anterior. */
function AcceptedDiff({ adjustment }: { adjustment: Adjustment }) {
  const { data: versions } = usePlanVersions();
  const after = versions?.find((v) => v.id === adjustment.planVersionId);
  const before = after ? versions?.find((v) => v.version === after.version - 1) : undefined;
  if (!after) return <p className="mt-3 text-small text-muted-foreground">A nova versão do plano ainda está sendo gerada.</p>;
  if (!before) return null;
  return (
    <details className="mt-4">
      <summary className="cursor-pointer text-small font-medium text-brand">
        Ver o que mudou (v{before.version} → v{after.version})
      </summary>
      <div className="mt-3">
        <PlanDiff fromId={before.id} toId={after.id} />
      </div>
    </details>
  );
}

/**
 * Reajustes propostos nos check-ins. Nada muda no plano sem a decisão da pessoa: aceitar gera uma versão
 * nova (com o motivo) e pede confirmação; recusar mantém o plano. A API não informa "impacto esperado".
 */
export function AdjustmentsSection() {
  const { data: items, error, isLoading, mutate: reload } = useAdjustments();
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const [confirm, setConfirm] = useState<Adjustment | null>(null);
  const [busy, setBusy] = useState(false);

  async function decide(adjustment: Adjustment, decision: "accept" | "reject") {
    setBusy(true);
    try {
      await decideAdjustment(adjustment.id, decision);
      await Promise.all([mutate(keys.adjustments), mutate(keys.planLatest), mutate(keys.planVersions), mutate(keys.planCurrent), mutate(keys.now)]);
      toast.success(decision === "accept" ? "Reajuste aceito: uma nova versão do plano está sendo gerada." : "Reajuste recusado: o plano continua o mesmo.");
      setConfirm(null);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Não consegui registrar a decisão.");
    } finally {
      setBusy(false);
    }
  }

  if (isLoading) return <Skeleton className="h-64" />;
  if (error || !items) return <ErrorState message="Não consegui carregar os reajustes." onRetry={() => void reload()} />;
  if (items.length === 0) {
    return (
      <Card>
        <EmptyState title="Nenhum reajuste por enquanto" description="Quando um check-in mostrar que o plano precisa mudar, a sugestão aparece aqui para você decidir." />
      </Card>
    );
  }

  const ordered = [...items].sort((a, b) => (a.status === "PROPOSED" ? -1 : 0) - (b.status === "PROPOSED" ? -1 : 0) || b.createdAt.localeCompare(a.createdAt));

  return (
    <>
      <ul className="space-y-4">
        {ordered.map((a) => {
          const status = STATUS[a.status] ?? { label: a.status, tone: "neutral" as const };
          return (
            <li key={a.id}>
              <Card>
                <CardHeader>
                  <div>
                    <CardDescription>Semana afetada: {shortDate.format(new Date(`${a.weekStart}T12:00:00`))}</CardDescription>
                    <CardTitle className="mt-1 text-body">{a.reason}</CardTitle>
                  </div>
                  <Badge tone={status.tone}>{status.label}</Badge>
                </CardHeader>
                {a.status === "PROPOSED" ? (
                  <>
                    <p className="text-small text-muted-foreground">Aceitar cria uma nova versão do plano com este motivo. Recusar não muda nada.</p>
                    <div className="mt-4 flex flex-wrap gap-2">
                      <Button onClick={() => setConfirm(a)}>Aceitar e refazer o plano</Button>
                      <Button variant="outline" disabled={busy} onClick={() => void decide(a, "reject")}>
                        Manter o plano
                      </Button>
                    </div>
                  </>
                ) : null}
                {a.status === "ACCEPTED" ? <AcceptedDiff adjustment={a} /> : null}
              </Card>
            </li>
          );
        })}
      </ul>
      <ConfirmDialog
        open={confirm !== null}
        onOpenChange={(open) => !open && setConfirm(null)}
        title="Aceitar este reajuste?"
        description={confirm ? `${confirm.reason} Uma nova versão do plano será gerada; a anterior continua no histórico.` : ""}
        confirmLabel="Aceitar reajuste"
        busy={busy}
        onConfirm={() => (confirm ? decide(confirm, "accept") : undefined)}
      />
    </>
  );
}
