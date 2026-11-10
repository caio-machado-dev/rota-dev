"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { ApiError, getGoal, saveGoal, type Goal, type GoalInput } from "@/lib/api/client";

const LEVELS = [
  ["JUNIOR", "Júnior"],
  ["PLENO", "Pleno"],
  ["SENIOR", "Sênior"],
  ["ESPECIALISTA", "Especialista"],
] as const;

const TRACKS = [
  ["BACKEND", "Backend"],
  ["FRONTEND", "Frontend"],
  ["FULLSTACK", "Fullstack"],
  ["DADOS", "Dados"],
  ["DEVOPS", "DevOps"],
  ["MOBILE", "Mobile"],
] as const;

type State = { status: "loading" } | { status: "anonymous" } | { status: "ready"; goal: Goal | null };

const fieldClass = "mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm";

/**
 * Formulário do objetivo de carreira (glossário). Os limites dos campos repetem os do
 * `GoalRequest` da API, para o navegador avisar antes de enviar; a API confere de novo.
 */
export function GoalForm({ onSaved }: { onSaved?: (goal: Goal) => void }) {
  const [state, setState] = useState<State>({ status: "loading" });
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    getGoal()
      .then((goal) => setState({ status: "ready", goal }))
      .catch((e) => setState(e instanceof ApiError && e.status === 401 ? { status: "anonymous" } : { status: "ready", goal: null }));
  }, []);

  if (state.status === "loading") return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (state.status === "anonymous") return null;

  const goal = state.goal;

  return (
    <form
      className="grid gap-4 rounded-lg border border-border p-4 md:grid-cols-2"
      onSubmit={async (event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const input: GoalInput = {
          targetRole: String(data.get("targetRole") ?? ""),
          targetLevel: data.get("targetLevel") as GoalInput["targetLevel"],
          track: data.get("track") as GoalInput["track"],
          deadline: String(data.get("deadline") ?? ""),
          hoursPerWeek: Number(data.get("hoursPerWeek")),
          primaryStack: String(data.get("primaryStack") ?? ""),
          wantsCertifications: data.get("wantsCertifications") === "on",
          notes: String(data.get("notes") ?? ""),
        };
        setPending(true);
        setError(null);
        setSaved(false);
        try {
          const result = await saveGoal(input);
          setState({ status: "ready", goal: result });
          setSaved(true);
          onSaved?.(result);
        } catch (e) {
          setError(e instanceof ApiError ? e.message : "Não consegui salvar.");
        } finally {
          setPending(false);
        }
      }}
    >
      <label className="text-sm md:col-span-2">
        Cargo-alvo
        <input name="targetRole" required maxLength={120} defaultValue={goal?.targetRole} placeholder="Ex.: Engenheiro Backend Java" className={fieldClass} />
      </label>
      <label className="text-sm">
        Nível
        <select name="targetLevel" defaultValue={goal?.targetLevel ?? "PLENO"} className={fieldClass}>
          {LEVELS.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className="text-sm">
        Trilha
        <select name="track" defaultValue={goal?.track ?? "BACKEND"} className={fieldClass}>
          {TRACKS.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className="text-sm">
        Prazo
        <input name="deadline" type="date" required defaultValue={goal?.deadline} className={fieldClass} />
      </label>
      <label className="text-sm">
        Horas por semana
        <input name="hoursPerWeek" type="number" min={1} max={60} required defaultValue={goal?.hoursPerWeek ?? 8} className={fieldClass} />
      </label>
      <label className="text-sm md:col-span-2">
        Stack principal
        <input name="primaryStack" maxLength={200} defaultValue={goal?.primaryStack ?? ""} placeholder="Ex.: Java, Spring, PostgreSQL" className={fieldClass} />
      </label>
      <label className="flex items-center gap-2 text-sm md:col-span-2">
        <input name="wantsCertifications" type="checkbox" defaultChecked={goal?.wantsCertifications} />
        Quero certificações quando agregarem
      </label>
      <label className="text-sm md:col-span-2">
        Observações
        <textarea name="notes" maxLength={2000} rows={3} defaultValue={goal?.notes ?? ""} className={fieldClass} />
      </label>
      <div className="flex items-center gap-3 md:col-span-2">
        <Button type="submit" disabled={pending}>
          {pending ? "Salvando…" : "Salvar objetivo"}
        </Button>
        {saved ? <span className="text-sm text-muted-foreground">Salvo.</span> : null}
        {error ? (
          <span role="alert" className="text-sm text-destructive">
            {error}
          </span>
        ) : null}
      </div>
    </form>
  );
}
