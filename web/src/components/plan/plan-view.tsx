"use client";

import { Flag } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { ApiError, getLatestPlan, listPlanVersions, requestPlan, type PlanVersion } from "@/lib/api/client";

const TASK_KIND: Record<string, string> = { STUDY: "Estudo", BUILD: "Construção", REVIEW: "Revisão", CERT: "Certificação" };
const RESOURCE_KIND: Record<string, string> = { COURSE: "Curso", BOOK: "Livro", CERT: "Certificação" };
const POLL_MS = 3000;

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });

/** Formata minutos como "1h30". */
function duration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, "0")}`;
}

/**
 * Plano (glossário): projeto-âncora, trimestres com portões e as semanas do primeiro trimestre.
 * Mostra a versão mais recente; o histórico lista todas, com o motivo de cada uma.
 */
export function PlanView() {
  const [plan, setPlan] = useState<PlanVersion | null | undefined>(undefined);
  const [versions, setVersions] = useState<PlanVersion[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const load = useCallback(async () => {
    try {
      const [latest, history] = await Promise.all([getLatestPlan(), listPlanVersions()]);
      setPlan(latest);
      setVersions(history);
    } catch {
      setPlan(null);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  useEffect(() => {
    if (plan?.status !== "PENDING") return;
    const timer = setTimeout(() => void load(), POLL_MS);
    return () => clearTimeout(timer);
  }, [plan, load]);

  const generate = async () => {
    setPending(true);
    setError(null);
    try {
      await requestPlan();
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui pedir o plano.");
    } finally {
      setPending(false);
    }
  };

  if (plan === undefined) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  const running = plan?.status === "PENDING";
  const content = plan?.status === "READY" ? plan.content : undefined;

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={generate} disabled={pending || running}>
          {running ? "Montando o plano…" : plan ? "Gerar nova versão" : "Gerar plano"}
        </Button>
        {plan ? (
          <span className="font-mono text-xs text-muted-foreground">
            v{plan.version} · começa em {dateFormat.format(new Date(`${plan.startsOn}T12:00:00`))}
          </span>
        ) : null}
        {error ? (
          <span role="alert" className="text-sm text-destructive">
            {error}
          </span>
        ) : null}
      </div>

      {plan?.status === "FAILED" ? (
        <p className="text-sm text-destructive">Não foi possível montar o plano. Tente de novo em alguns minutos.</p>
      ) : null}

      {content ? (
        <>
          <p className="text-sm leading-relaxed">{content.summary}</p>

          <section aria-labelledby="ancora" className="rounded-lg border border-border p-4">
            <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground">Projeto-âncora</p>
            <h3 id="ancora" className="mt-1 font-medium">
              {content.anchorProject.title}
            </h3>
            <p className="mt-1 text-sm text-muted-foreground">{content.anchorProject.description}</p>
            {content.anchorProject.deliverables.length > 0 ? (
              <ul className="mt-3 list-disc pl-5 text-sm">
                {content.anchorProject.deliverables.map((d) => (
                  <li key={d}>{d}</li>
                ))}
              </ul>
            ) : null}
          </section>

          <section aria-labelledby="trimestres">
            <h3 id="trimestres" className="text-sm font-semibold">
              Trimestres e portões
            </h3>
            <ol className="mt-3 flex flex-col gap-4">
              {content.quarters.map((q) => (
                <li key={q.index} className="rounded-lg border border-border p-4">
                  <p className="font-mono text-xs text-muted-foreground">Trimestre {q.index}</p>
                  <p className="mt-1 font-medium">{q.title}</p>
                  <p className="text-sm text-muted-foreground">{q.focus}</p>
                  {q.milestones.map((m) => (
                    <div key={m.title} className="mt-3 text-sm">
                      <p className="flex items-center gap-2 font-medium">
                        <Flag className="size-3.5 text-brand" aria-hidden />
                        Portão: {m.title}
                      </p>
                      <ul className="mt-1 list-disc pl-9 text-muted-foreground">
                        {m.criteria.map((c) => (
                          <li key={c}>{c}</li>
                        ))}
                      </ul>
                    </div>
                  ))}
                  {q.weeks.length > 0 ? (
                    <details className="mt-4">
                      <summary className="cursor-pointer text-sm text-muted-foreground">Semanas ({q.weeks.length})</summary>
                      <ol className="mt-2 flex flex-col gap-3">
                        {q.weeks.map((w) => (
                          <li key={w.index} className="text-sm">
                            <p className="flex justify-between gap-2">
                              <span className="font-medium">
                                Semana {w.index} · {w.theme}
                              </span>
                              <span className="font-mono text-xs text-muted-foreground">{duration(w.totalMinutes)}</span>
                            </p>
                            <ul className="mt-1 flex flex-col gap-0.5 text-muted-foreground">
                              {w.tasks.map((t) => (
                                <li key={t.title} className="flex justify-between gap-2">
                                  <span>
                                    {TASK_KIND[t.kind] ?? t.kind}: {t.title}
                                  </span>
                                  <span className="shrink-0 font-mono text-xs">{duration(t.minutes)}</span>
                                </li>
                              ))}
                            </ul>
                          </li>
                        ))}
                      </ol>
                    </details>
                  ) : null}
                </li>
              ))}
            </ol>
          </section>

          {content.resources.length > 0 ? (
            <section aria-labelledby="recursos">
              <h3 id="recursos" className="text-sm font-semibold">
                Recursos que agregam
              </h3>
              <ul className="mt-2 flex flex-col gap-1 text-sm">
                {content.resources.map((r) => (
                  <li key={r.title}>
                    <span className="font-medium">{r.title}</span>{" "}
                    <span className="text-muted-foreground">
                      ({RESOURCE_KIND[r.kind] ?? r.kind}) · {r.reason}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}

      {versions.length > 1 ? (
        <section aria-labelledby="historico">
          <h3 id="historico" className="text-sm font-semibold">
            Histórico de versões
          </h3>
          <ul className="mt-2 flex flex-col gap-1 text-sm text-muted-foreground">
            {versions.map((v) => (
              <li key={v.id}>
                <span className="font-mono">v{v.version}</span> · {dateFormat.format(new Date(v.createdAt))} · {v.reason}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}
