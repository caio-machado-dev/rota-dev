"use client";

import { Flag } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { Button } from "@/components/ui/button";
import {
  achieveMilestone,
  ApiError,
  getCheckinStatus,
  getCurrentPlan,
  getReadiness,
  listMilestones,
  LOGIN_URL,
  submitCheckin,
  type CheckinStatus,
  type MilestoneAchievement,
  type PlanVersion,
  type Readiness,
} from "@/lib/api/client";

const PARTS = [
  ["skills", "Habilidades", 40],
  ["gates", "Portões", 25],
  ["study", "Estudo", 20],
  ["habit", "Hábito", 15],
] as const;

const fieldClass = "mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm";
const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function duration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, "0")}`;
}

type Data = { readiness: Readiness; checkin: CheckinStatus; plan: PlanVersion | null; achieved: MilestoneAchievement[] };
type State = { status: "loading" } | { status: "anonymous" } | { status: "error" } | { status: "ready"; data: Data };

/** Progresso: índice de prontidão com as partes, a evolução por check-in, o check-in e os portões. */
export function ProgressView() {
  const [state, setState] = useState<State>({ status: "loading" });

  const load = useCallback(async () => {
    try {
      const [readiness, checkin, plan, achieved] = await Promise.all([getReadiness(), getCheckinStatus(), getCurrentPlan(), listMilestones()]);
      setState({ status: "ready", data: { readiness, checkin, plan, achieved } });
    } catch (e) {
      setState(e instanceof ApiError && e.status === 401 ? { status: "anonymous" } : { status: "error" });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  if (state.status === "loading") return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (state.status === "error") return <p className="text-sm text-destructive">Não consegui carregar o progresso.</p>;
  if (state.status === "anonymous") {
    return (
      <p className="text-sm text-muted-foreground">
        <a href={LOGIN_URL} className="font-medium text-brand hover:underline">
          Entre com GitHub
        </a>{" "}
        para ver o seu progresso.
      </p>
    );
  }

  const { readiness, checkin, plan, achieved } = state.data;
  return (
    <div className="flex flex-col gap-10">
      <ReadinessSection readiness={readiness} />
      <CheckinSection checkin={checkin} onDone={load} />
      <MilestonesSection plan={plan} achieved={achieved} onDone={load} />
    </div>
  );
}

function ReadinessSection({ readiness }: { readiness: Readiness }) {
  const { current, history } = readiness;
  const data = history.map((p) => ({ week: shortDate.format(new Date(`${p.weekStart}T12:00:00`)), score: p.value.score }));

  return (
    <section aria-labelledby="prontidao">
      <h2 id="prontidao" className="text-sm font-semibold">
        Índice de prontidão
      </h2>
      <p className="mt-2 flex items-baseline gap-2">
        <span className="text-5xl font-semibold tabular-nums">{current.score}</span>
        <span className="text-sm text-muted-foreground">de 100</span>
      </p>

      <ul className="mt-4 grid max-w-xl gap-2 text-sm">
        {PARTS.map(([key, label, weight]) => (
          <li key={key} className="grid grid-cols-[9.5rem_1fr_3rem] items-center gap-3">
            <span>
              {label} <span className="text-xs text-muted-foreground">· peso {weight}</span>
            </span>
            <span className="h-1.5 rounded-full bg-muted" aria-hidden>
              <span className="block h-full rounded-full bg-viz-current" style={{ width: `${current[key]}%` }} />
            </span>
            <span className="text-right font-mono text-xs text-muted-foreground">{current[key]}%</span>
          </li>
        ))}
      </ul>

      {data.length > 1 ? (
        <figure className="mt-6 h-56 w-full max-w-xl" aria-label="Evolução do índice de prontidão por check-in">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
              <CartesianGrid stroke="var(--border)" vertical={false} />
              <XAxis dataKey="week" tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 100]} ticks={[0, 50, 100]} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }}
                labelStyle={{ color: "var(--foreground)" }}
                itemStyle={{ color: "var(--foreground)" }}
                formatter={(value) => [value, "Índice"]}
              />
              <Line type="monotone" dataKey="score" stroke="var(--viz-current)" strokeWidth={2} dot={{ r: 4 }} activeDot={{ r: 5 }} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </figure>
      ) : (
        <p className="mt-4 text-sm text-muted-foreground">A linha do tempo aparece a partir do segundo check-in.</p>
      )}
      {/* Os mesmos números em texto, para leitor de tela e para quem não distingue a cor. */}
      {history.length > 0 ? (
        <table className="sr-only">
          <caption>Índice por semana</caption>
          <tbody>
            {history.map((p) => (
              <tr key={p.weekStart}>
                <td>{p.weekStart}</td>
                <td>{p.value.score}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </section>
  );
}

function CheckinSection({ checkin, onDone }: { checkin: CheckinStatus; onDone: () => Promise<void> }) {
  const [error, setError] = useState<string | null>(null);

  if (!checkin.openWeek) {
    return (
      <section aria-labelledby="checkin">
        <h2 id="checkin" className="text-sm font-semibold">
          Check-in semanal
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">O check-in abre no sábado e fica aberto até terça.</p>
      </section>
    );
  }

  const week = shortDate.format(new Date(`${checkin.openWeek}T12:00:00`));
  return (
    <section aria-labelledby="checkin">
      <h2 id="checkin" className="text-sm font-semibold">
        Check-in da semana de {week}
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">
        {duration(checkin.minutesLogged)} registradas · {checkin.reviewDays} {checkin.reviewDays === 1 ? "dia" : "dias"} com o protocolo
        completo
      </p>
      {checkin.done ? (
        <p className="mt-3 text-sm">
          Feito: {checkin.done.commits} commits, energia {checkin.done.energy}/5.
          {checkin.done.blockers ? <span className="text-muted-foreground"> Bloqueios: {checkin.done.blockers}</span> : null}
        </p>
      ) : (
        <form
          className="mt-3 grid max-w-xl gap-3 md:grid-cols-2"
          onSubmit={async (event) => {
            event.preventDefault();
            const data = new FormData(event.currentTarget);
            setError(null);
            try {
              await submitCheckin({
                commits: Number(data.get("commits")),
                energy: Number(data.get("energy")),
                blockers: String(data.get("blockers") ?? ""),
                notes: String(data.get("notes") ?? ""),
              });
              await onDone();
            } catch (e) {
              setError(e instanceof ApiError ? e.message : "Não consegui salvar o check-in.");
            }
          }}
        >
          <label className="text-sm">
            Commits na semana
            <input name="commits" type="number" min={0} max={1000} required defaultValue={0} className={fieldClass} />
          </label>
          <label className="text-sm">
            Energia (1 a 5)
            <input name="energy" type="number" min={1} max={5} required defaultValue={3} className={fieldClass} />
          </label>
          <label className="text-sm md:col-span-2">
            O que travou
            <input name="blockers" maxLength={2000} className={fieldClass} />
          </label>
          <label className="text-sm md:col-span-2">
            Notas
            <textarea name="notes" maxLength={2000} rows={2} className={fieldClass} />
          </label>
          <div className="flex items-center gap-3 md:col-span-2">
            <Button type="submit" size="sm">
              Fechar a semana
            </Button>
            {error ? (
              <span role="alert" className="text-sm text-destructive">
                {error}
              </span>
            ) : null}
          </div>
        </form>
      )}
    </section>
  );
}

function MilestonesSection({
  plan,
  achieved,
  onDone,
}: {
  plan: PlanVersion | null;
  achieved: MilestoneAchievement[];
  onDone: () => Promise<void>;
}) {
  const [error, setError] = useState<string | null>(null);
  const done = new Map(achieved.map((a) => [a.milestone, a]));
  const milestones = plan?.content?.quarters.flatMap((q) => q.milestones.map((m) => ({ ...m, quarter: q.index }))) ?? [];
  const pending = milestones.filter((m) => !done.has(m.title));

  return (
    <section aria-labelledby="portoes">
      <h2 id="portoes" className="text-sm font-semibold">
        Portões{" "}
        {milestones.length > 0 ? (
          <span className="font-normal text-muted-foreground">
            · {milestones.filter((m) => done.has(m.title)).length} de {milestones.length}
          </span>
        ) : null}
      </h2>
      {milestones.length === 0 ? (
        <p className="mt-2 text-sm text-muted-foreground">Os portões aparecem quando houver um plano em vigor.</p>
      ) : (
        <ul className="mt-3 flex flex-col gap-2 text-sm">
          {milestones.map((m) => {
            const a = done.get(m.title);
            return (
              <li key={m.title} className="flex items-start gap-2">
                <Flag className={a ? "mt-0.5 size-3.5 text-brand" : "mt-0.5 size-3.5 text-muted-foreground"} aria-hidden />
                <span>
                  <span className={a ? "font-medium" : ""}>{m.title}</span>{" "}
                  <span className="text-muted-foreground">· trimestre {m.quarter}</span>
                  {a ? (
                    <>
                      {" · "}
                      <a href={a.evidenceUrl} target="_blank" rel="noreferrer" className="text-brand hover:underline">
                        prova
                      </a>
                    </>
                  ) : null}
                </span>
              </li>
            );
          })}
        </ul>
      )}
      {pending.length > 0 ? (
        <form
          className="mt-4 grid max-w-xl gap-3"
          onSubmit={async (event) => {
            event.preventDefault();
            const form = event.currentTarget;
            const data = new FormData(form);
            setError(null);
            try {
              await achieveMilestone(String(data.get("milestone")), String(data.get("evidenceUrl")));
              form.reset();
              await onDone();
            } catch (e) {
              setError(e instanceof ApiError ? e.message : "Não consegui registrar o portão.");
            }
          }}
        >
          <label className="text-sm">
            Portão alcançado
            <select name="milestone" className={fieldClass}>
              {pending.map((m) => (
                <option key={m.title} value={m.title}>
                  {m.title}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            Link da prova (PR, deploy, repositório)
            <input name="evidenceUrl" type="url" required pattern="https://.*" placeholder="https://github.com/…" className={fieldClass} />
          </label>
          <div className="flex items-center gap-3">
            <Button type="submit" size="sm" variant="outline">
              Registrar portão
            </Button>
            {error ? (
              <span role="alert" className="text-sm text-destructive">
                {error}
              </span>
            ) : null}
          </div>
        </form>
      ) : null}
    </section>
  );
}
