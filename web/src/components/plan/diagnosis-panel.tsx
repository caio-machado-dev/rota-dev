"use client";

import { ExternalLink } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Legend, PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, ResponsiveContainer, Tooltip } from "recharts";

import { Button } from "@/components/ui/button";
import { ApiError, getLatestDiagnosis, requestDiagnosis, type Diagnosis } from "@/lib/api/client";

const LEVEL_LABEL: Record<string, string> = {
  JUNIOR: "Júnior",
  PLENO: "Pleno",
  SENIOR: "Sênior",
  ESPECIALISTA: "Especialista",
  NAO_ESTIMADO: "Não estimado",
};

/** Enquanto o job roda, consulta a API a cada 3 segundos. */
const POLL_MS = 3000;

/**
 * Diagnóstico (glossário): radar atual × exigido, nota por eixo com as evidências citadas e as
 * lacunas em ordem de prioridade. O radar nunca é a única fonte: cada eixo aparece em texto com os
 * números, e "exigido" é tracejado, para não depender só da cor.
 */
export function DiagnosisPanel() {
  const [diagnosis, setDiagnosis] = useState<Diagnosis | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const load = useCallback(async () => {
    try {
      setDiagnosis(await getLatestDiagnosis());
    } catch {
      setDiagnosis(null);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  useEffect(() => {
    if (diagnosis?.status !== "PENDING") return;
    const timer = setTimeout(() => void load(), POLL_MS);
    return () => clearTimeout(timer);
  }, [diagnosis, load]);

  const generate = async () => {
    setPending(true);
    setError(null);
    try {
      await requestDiagnosis();
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui pedir o diagnóstico.");
    } finally {
      setPending(false);
    }
  };

  if (diagnosis === undefined) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  const running = diagnosis?.status === "PENDING";
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={generate} disabled={pending || running}>
          {running ? "Gerando diagnóstico…" : diagnosis ? "Gerar de novo" : "Gerar diagnóstico"}
        </Button>
        {error ? (
          <span role="alert" className="text-sm text-destructive">
            {error}
          </span>
        ) : null}
      </div>

      {diagnosis?.status === "FAILED" ? (
        <p className="text-sm text-destructive">Não foi possível gerar o diagnóstico. Tente de novo em alguns minutos.</p>
      ) : null}
      {diagnosis?.status === "READY" ? <DiagnosisView diagnosis={diagnosis} /> : null}
    </div>
  );
}

/** Quebra o nome do eixo em linhas de até ~12 caracteres, para não cortar na tela do celular. */
function wrap(text: string, max = 12): string[] {
  const lines: string[] = [];
  for (const word of text.split(" ")) {
    const last = lines.at(-1);
    if (last !== undefined && `${last} ${word}`.length <= max) lines[lines.length - 1] = `${last} ${word}`;
    else lines.push(word);
  }
  return lines;
}

type TickProps = {
  x?: number | string;
  y?: number | string;
  textAnchor?: "start" | "middle" | "end" | "inherit";
  payload?: { value: string };
};

function AxisLabel({ x = 0, y = 0, textAnchor = "middle", payload }: TickProps) {
  const lines = wrap(payload?.value ?? "");
  // Rótulos acima do centro sobem, para a segunda linha não encostar no gráfico.
  const offset = Number(y) < 150 ? -(lines.length - 1) * 14 : 0;
  return (
    <text x={x} y={Number(y) + offset} textAnchor={textAnchor} fill="var(--muted-foreground)" fontSize={12}>
      {lines.map((line, i) => (
        <tspan key={i} x={x} dy={i === 0 ? 0 : 14}>
          {line}
        </tspan>
      ))}
    </text>
  );
}

function DiagnosisView({ diagnosis }: { diagnosis: Diagnosis }) {
  const data = diagnosis.axes.map((a) => ({ axis: a.name, Atual: a.score, Exigido: a.required }));

  return (
    <div className="flex flex-col gap-8">
      <div>
        <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
          Nível estimado: {LEVEL_LABEL[diagnosis.estimatedLevel ?? ""] ?? diagnosis.estimatedLevel}
        </p>
        <p className="mt-2 text-sm leading-relaxed">{diagnosis.summary}</p>
      </div>

      <figure aria-label="Radar de competências: nota atual e nota exigida, de 0 a 5" className="h-80 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <RadarChart data={data} outerRadius="62%">
            <PolarGrid stroke="var(--border)" />
            <PolarAngleAxis dataKey="axis" tick={AxisLabel} />
            <PolarRadiusAxis domain={[0, 5]} tickCount={6} tick={false} axisLine={false} />
            <Radar
              name="Exigido"
              dataKey="Exigido"
              stroke="var(--viz-required)"
              strokeWidth={2}
              strokeDasharray="6 4"
              fill="none"
            />
            <Radar name="Atual" dataKey="Atual" stroke="var(--viz-current)" strokeWidth={2} fill="var(--viz-current)" fillOpacity={0.3} />
            {/* O texto da legenda fica na cor de texto; a cor da série só no marcador ao lado. */}
            <Legend
              wrapperStyle={{ fontSize: 12 }}
              formatter={(value: string) => <span style={{ color: "var(--muted-foreground)" }}>{value}</span>}
            />
            <Tooltip
              contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }}
              labelStyle={{ color: "var(--foreground)" }}
              itemStyle={{ color: "var(--foreground)" }}
            />
          </RadarChart>
        </ResponsiveContainer>
      </figure>

      <section aria-labelledby="eixos">
        <h3 id="eixos" className="text-sm font-semibold">
          Por competência
        </h3>
        <ul className="mt-3 flex flex-col gap-3">
          {diagnosis.axes.map((a) => (
            <li key={a.key} className="rounded-lg border border-border p-3 text-sm">
              <div className="flex items-baseline justify-between gap-3">
                <span className="font-medium">{a.name}</span>
                <span className="font-mono text-xs text-muted-foreground">
                  {a.score} de {a.required}
                  {a.verified ? "" : " · não verificado"}
                </span>
              </div>
              {a.rationale ? <p className="mt-1 text-muted-foreground">{a.rationale}</p> : null}
              {a.evidences.length > 0 ? (
                <ul className="mt-2 flex flex-wrap gap-2 text-xs">
                  {a.evidences.map((e) =>
                    e.url ? (
                      <li key={e.id}>
                        <a href={e.url} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-brand hover:underline">
                          {e.title ?? "evidência"}
                          <ExternalLink className="size-3" aria-hidden />
                        </a>
                      </li>
                    ) : (
                      <li key={e.id} className="text-muted-foreground">
                        {e.title ?? "evidência de uma leitura anterior"}
                      </li>
                    ),
                  )}
                </ul>
              ) : null}
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="lacunas">
        <h3 id="lacunas" className="text-sm font-semibold">
          Lacunas, por prioridade
        </h3>
        {diagnosis.gaps.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground">Nenhuma lacuna: você já atende o que o cargo-alvo pede.</p>
        ) : (
          <ol className="mt-3 flex flex-col gap-2 text-sm">
            {diagnosis.gaps.map((g) => (
              <li key={g.axisKey} className="flex gap-3">
                <span className="font-mono text-xs text-muted-foreground">{g.priority}.</span>
                <span>
                  <span className="font-medium">{g.axisName}</span>{" "}
                  <span className="text-muted-foreground">
                    (faltam {g.size} {g.size === 1 ? "ponto" : "pontos"}) · {g.description}
                  </span>
                </span>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}
