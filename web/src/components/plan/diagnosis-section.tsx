"use client";

import { Sparkles } from "lucide-react";
import { useState } from "react";
import { useSWRConfig } from "swr";

import { SkillGapChart } from "@/components/charts/skill-gap-chart";
import { SkillRadar } from "@/components/charts/skill-radar";
import { JobBanner } from "@/components/system/job-banner";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError, requestDiagnosis, type Diagnosis } from "@/lib/api/client";
import { keys, useGoal, useLatestDiagnosis } from "@/lib/api/hooks";
import { cn } from "@/lib/utils";

import { SkillDetail } from "./skill-detail";

const LEVEL_LABEL: Record<string, string> = { JUNIOR: "Júnior", PLENO: "Pleno", SENIOR: "Sênior", ESPECIALISTA: "Especialista", NAO_ESTIMADO: "Não estimado" };

/** Compatibilidade com o cargo: soma do que já atende (limitado ao exigido) sobre a soma do exigido. Derivada das notas, sem peso oculto. */
export function compatibility(axes: Diagnosis["axes"]): number {
  const required = axes.reduce((sum, a) => sum + a.required, 0);
  if (required === 0) return 100;
  return Math.round((axes.reduce((sum, a) => sum + Math.min(a.score, a.required), 0) / required) * 100);
}

function DiagnosisHeader({ diagnosis }: { diagnosis: Diagnosis }) {
  const match = compatibility(diagnosis.axes);
  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-6">
        <div className="min-w-0 max-w-2xl">
          <Badge tone="brand">Nível estimado: {LEVEL_LABEL[diagnosis.estimatedLevel ?? ""] ?? diagnosis.estimatedLevel ?? "—"}</Badge>
          <p className="mt-3 text-body leading-relaxed">{diagnosis.summary}</p>
        </div>
        <div className="shrink-0 text-right">
          <p className="num text-display font-semibold leading-none">
            {match}
            <span className="text-h3 font-normal text-muted-foreground">%</span>
          </p>
          <p className="mt-2 text-small text-muted-foreground">compatibilidade com o cargo</p>
        </div>
      </div>
      <p className="mt-4 border-t border-border pt-3 text-micro text-muted-foreground">
        Cálculo: soma das notas atuais (até o exigido) dividida pela soma das notas exigidas, em todas as competências.
      </p>
    </Card>
  );
}

function DiagnosisView({ diagnosis }: { diagnosis: Diagnosis }) {
  const [selected, setSelected] = useState<string | null>(diagnosis.gaps[0]?.axisKey ?? diagnosis.axes[0]?.key ?? null);
  const axis = diagnosis.axes.find((a) => a.key === selected) ?? null;

  return (
    <div className="space-y-6">
      <DiagnosisHeader diagnosis={diagnosis} />

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Atual × exigido</CardTitle>
              <CardDescription>Clique numa competência para ver a justificativa e as evidências.</CardDescription>
            </div>
          </CardHeader>
          <SkillRadar axes={diagnosis.axes} selectedKey={selected} onSelect={setSelected} />
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div>
                <CardTitle>Competências</CardTitle>
                <CardDescription>Nota atual de 0 a 5 e o que o cargo exige.</CardDescription>
              </div>
            </CardHeader>
            <ul className="space-y-1">
              {diagnosis.axes.map((a) => (
                <li key={a.key}>
                  <button
                    type="button"
                    aria-pressed={a.key === selected}
                    onClick={() => setSelected(a.key)}
                    className={cn(
                      "flex w-full items-center justify-between gap-3 rounded-control px-3 py-2 text-left text-body outline-none transition-colors hover:bg-accent/60 focus-visible:ring-2 focus-visible:ring-ring",
                      a.key === selected && "bg-accent",
                    )}
                  >
                    <span>{a.name}</span>
                    <span className="num flex items-center gap-2 text-small text-muted-foreground">
                      {a.score} de {a.required}
                      {a.verified ? null : <Badge tone="warning">Não verificado</Badge>}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </Card>
          {axis ? <SkillDetail axis={axis} /> : null}
        </div>
      </div>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Lacunas prioritárias</CardTitle>
            <CardDescription>Onde investir primeiro para chegar ao cargo-alvo.</CardDescription>
          </div>
        </CardHeader>
        {diagnosis.gaps.length === 0 ? (
          <p className="text-small text-muted-foreground">Nenhuma lacuna: você já atende o que o cargo-alvo pede.</p>
        ) : (
          <SkillGapChart gaps={diagnosis.gaps} />
        )}
      </Card>
      <p className="text-micro text-muted-foreground">Contestar uma nota ainda não é possível: a API não oferece essa operação.</p>
    </div>
  );
}

/** Aba "Diagnóstico": gera (job), acompanha o status real e mostra o resultado com evidências. */
export function DiagnosisSection() {
  const { data: diagnosis, error, isLoading, mutate: reload } = useLatestDiagnosis();
  const { data: goal } = useGoal();
  const { mutate } = useSWRConfig();
  const [requestError, setRequestError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function generate() {
    setPending(true);
    setRequestError(null);
    try {
      await requestDiagnosis();
      await mutate(keys.diagnosis);
    } catch (e) {
      setRequestError(e instanceof ApiError ? e.message : "Não consegui pedir o diagnóstico.");
    } finally {
      setPending(false);
    }
  }

  if (isLoading) return <Skeleton className="h-96" />;
  if (error) return <ErrorState message="Não consegui carregar o diagnóstico." onRetry={() => void reload()} />;

  const running = diagnosis?.status === "PENDING";
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={() => void generate()} disabled={pending || running || !goal}>
          <Sparkles aria-hidden /> {running ? "Gerando…" : diagnosis ? "Gerar de novo" : "Gerar diagnóstico"}
        </Button>
        {!goal ? <span className="text-small text-muted-foreground">Defina o objetivo antes de gerar.</span> : null}
        {requestError ? (
          <span role="alert" className="text-small text-destructive">
            {requestError}
          </span>
        ) : null}
      </div>

      <JobBanner
        status={diagnosis?.status}
        runningLabel="Lendo suas fontes e cruzando com o cargo-alvo."
        failedMessage="Não foi possível gerar o diagnóstico. Tente de novo em alguns minutos."
        onRetry={() => void generate()}
      />

      {diagnosis?.status === "READY" ? (
        <DiagnosisView diagnosis={diagnosis} />
      ) : !diagnosis ? (
        <Card>
          <EmptyState
            title="Ainda não há diagnóstico"
            description="Ele compara o que você já construiu com o que o cargo-alvo exige, competência por competência, com as evidências."
          />
        </Card>
      ) : null}
    </div>
  );
}
