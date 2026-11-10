"use client";

import { Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  ApiError,
  deleteSession,
  getNow,
  LOGIN_URL,
  logSession,
  saveHabit,
  type Now,
  type SessionInput,
} from "@/lib/api/client";

const WEEKDAY = ["", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];
const KIND: Record<string, string> = { STUDY: "Estudo", BUILD: "Construção", REVIEW: "Revisão", CERT: "Certificação" };
const HABIT_STEPS = [
  ["readDiff", "Li o diff inteiro"],
  ["explained", "Consigo explicar o que ele faz"],
  ["tested", "Escrevi ou rodei um teste que prova"],
] as const;

const fieldClass = "mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm";

function duration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, "0")}`;
}

type State = { status: "loading" } | { status: "anonymous" } | { status: "error" } | { status: "ready"; now: Now };

/** Painel "Agora": o que fazer hoje e o registro do dia, tudo na data do fuso do usuário. */
export function NowPanel() {
  const [state, setState] = useState<State>({ status: "loading" });

  const load = useCallback(async () => {
    try {
      setState({ status: "ready", now: await getNow() });
    } catch (e) {
      setState(e instanceof ApiError && e.status === 401 ? { status: "anonymous" } : { status: "error" });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  if (state.status === "loading") return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (state.status === "error") return <p className="text-sm text-destructive">Não consegui carregar o dia. Tente de novo.</p>;
  if (state.status === "anonymous") {
    return (
      <p className="text-sm text-muted-foreground">
        <a href={LOGIN_URL} className="font-medium text-brand hover:underline">
          Entre com GitHub
        </a>{" "}
        para começar.
      </p>
    );
  }

  const { now } = state;
  const budget = now.weeklyBudgetMinutes;
  const share = budget ? Math.min(1, now.minutesThisWeek / budget) : 0;

  return (
    <div className="flex flex-col gap-10">
      <header>
        <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground">
          {WEEKDAY[now.weekday]} · {new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(`${now.today}T12:00:00`))}
        </p>
        <div className="mt-3 flex items-baseline gap-2">
          <span className="text-3xl font-semibold tabular-nums">{duration(now.minutesThisWeek)}</span>
          <span className="text-sm text-muted-foreground">{budget ? `de ${duration(budget)} nesta semana` : "nesta semana"}</span>
        </div>
        {budget ? (
          <div
            className="mt-2 h-1.5 max-w-sm rounded-full bg-muted"
            role="progressbar"
            aria-label="Horas da semana"
            aria-valuemin={0}
            aria-valuemax={budget}
            aria-valuenow={now.minutesThisWeek}
          >
            <div className="h-full rounded-full bg-brand" style={{ width: `${Math.round(share * 100)}%` }} />
          </div>
        ) : null}
      </header>

      <section aria-labelledby="semana-plano">
        <h2 id="semana-plano" className="text-sm font-semibold">
          {now.planWeek ? `Semana ${now.planWeek.index} do plano · ${now.planWeek.theme}` : "Semana do plano"}
        </h2>
        {now.planWeek ? (
          <ul className="mt-2 flex flex-col gap-1 text-sm">
            {now.planWeek.tasks.map((t) => (
              <li key={t.title} className="flex justify-between gap-3">
                <span>
                  <span className="text-muted-foreground">{KIND[t.kind] ?? t.kind}:</span> {t.title}
                </span>
                <span className="shrink-0 font-mono text-xs text-muted-foreground">{duration(t.minutes)}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-muted-foreground">
            Sem plano em vigor para esta semana. Monte o seu em{" "}
            <a href="/plano" className="text-brand hover:underline">
              Plano
            </a>
            .
          </p>
        )}
        {now.templateToday.length > 0 ? (
          <p className="mt-3 text-sm text-muted-foreground">
            Sua semana padrão para hoje:{" "}
            {now.templateToday.map((s) => `${KIND[s.kind] ?? s.kind} ${duration(s.minutes)}`).join(" · ")}
          </p>
        ) : null}
      </section>

      <SessionSection now={now} onChange={load} />
      <HabitSection now={now} onChange={load} />
    </div>
  );
}

function SessionSection({ now, onChange }: { now: Now; onChange: () => Promise<void> }) {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  return (
    <section aria-labelledby="sessoes">
      <h2 id="sessoes" className="text-sm font-semibold">
        Sessões de hoje {now.minutesToday > 0 ? <span className="font-normal text-muted-foreground">· {duration(now.minutesToday)}</span> : null}
      </h2>
      {now.sessionsToday.length > 0 ? (
        <ul className="mt-2 flex flex-col gap-2 text-sm">
          {now.sessionsToday.map((s) => (
            <li key={s.id} className="flex items-start justify-between gap-3 rounded-md border border-border px-3 py-2">
              <span>
                <span className="font-medium">{s.topic}</span>{" "}
                <span className="text-muted-foreground">
                  · {KIND[s.kind] ?? s.kind} · {duration(s.minutes)}
                </span>
                {s.learning ? <span className="block text-muted-foreground">{s.learning}</span> : null}
              </span>
              <button
                type="button"
                aria-label={`Apagar sessão ${s.topic}`}
                className="rounded p-1 text-muted-foreground hover:text-foreground"
                onClick={async () => {
                  await deleteSession(s.id);
                  await onChange();
                }}
              >
                <Trash2 className="size-4" aria-hidden />
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      <form
        className="mt-4 grid gap-3 rounded-lg border border-border p-4 md:grid-cols-[1fr_8rem_8rem]"
        onSubmit={async (event) => {
          event.preventDefault();
          const form = event.currentTarget;
          const data = new FormData(form);
          const input: SessionInput = {
            kind: data.get("kind") as SessionInput["kind"],
            minutes: Number(data.get("minutes")),
            topic: String(data.get("topic") ?? ""),
            learning: String(data.get("learning") ?? ""),
          };
          setPending(true);
          setError(null);
          try {
            await logSession(input);
            form.reset();
            await onChange();
          } catch (e) {
            setError(e instanceof ApiError ? e.message : "Não consegui registrar.");
          } finally {
            setPending(false);
          }
        }}
      >
        <label className="text-sm">
          Tema
          <input name="topic" required maxLength={200} placeholder="Ex.: transações no Spring" className={fieldClass} />
        </label>
        <label className="text-sm">
          Tipo
          <select name="kind" defaultValue="STUDY" className={fieldClass}>
            {Object.entries(KIND).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          Minutos
          <input name="minutes" type="number" min={5} max={600} required defaultValue={30} className={fieldClass} />
        </label>
        <label className="text-sm md:col-span-3">
          O que aprendi (uma linha)
          <input name="learning" maxLength={500} className={fieldClass} />
        </label>
        <div className="flex items-center gap-3 md:col-span-3">
          <Button type="submit" size="sm" disabled={pending}>
            Registrar sessão
          </Button>
          {error ? (
            <span role="alert" className="text-sm text-destructive">
              {error}
            </span>
          ) : null}
        </div>
      </form>
    </section>
  );
}

function HabitSection({ now, onChange }: { now: Now; onChange: () => Promise<void> }) {
  const habit = now.habitToday;
  const [saved, setSaved] = useState(false);

  return (
    <section aria-labelledby="protocolo">
      <h2 id="protocolo" className="text-sm font-semibold">
        Protocolo anti vibe coding{" "}
        <span className="font-normal text-muted-foreground">· {habit?.stepsDone ?? 0} de 4</span>
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Ao revisar código escrito por IA hoje: leia, explique, prove com um teste e anote o que aprendeu.
      </p>
      <form
        // A chave muda quando o registro salvo muda, para os campos recomeçarem dos valores da API.
        key={`${habit?.stepsDone ?? 0}-${habit?.note ?? ""}`}
        className="mt-3 flex flex-col gap-2 text-sm"
        onSubmit={async (event) => {
          event.preventDefault();
          const data = new FormData(event.currentTarget);
          await saveHabit({
            day: now.today,
            readDiff: data.get("readDiff") === "on",
            explained: data.get("explained") === "on",
            tested: data.get("tested") === "on",
            note: String(data.get("note") ?? ""),
          });
          setSaved(true);
          await onChange();
        }}
      >
        {HABIT_STEPS.map(([name, label]) => (
          <label key={name} className="flex items-center gap-2">
            <input type="checkbox" name={name} defaultChecked={habit?.[name] ?? false} />
            {label}
          </label>
        ))}
        <label>
          Nota do que aprendi
          <input name="note" maxLength={1000} defaultValue={habit?.note ?? ""} className={fieldClass} />
        </label>
        <div className="flex items-center gap-3">
          <Button type="submit" size="sm" variant="outline">
            Salvar protocolo
          </Button>
          {habit?.complete ? <span className="text-brand">Dia completo.</span> : saved ? <span className="text-muted-foreground">Salvo.</span> : null}
        </div>
      </form>
    </section>
  );
}
