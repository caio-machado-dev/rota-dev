"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Field, Input, Select, Textarea } from "@/components/ui/input";
import { ApiError, saveGoal, type Goal, type GoalInput } from "@/lib/api/client";

export const LEVELS = [
  ["JUNIOR", "Júnior"],
  ["PLENO", "Pleno"],
  ["SENIOR", "Sênior"],
  ["ESPECIALISTA", "Especialista"],
] as const;

export const TRACKS = [
  ["BACKEND", "Backend"],
  ["FRONTEND", "Frontend"],
  ["FULLSTACK", "Fullstack"],
  ["DADOS", "Dados"],
  ["DEVOPS", "DevOps"],
  ["MOBILE", "Mobile"],
] as const;

/**
 * Formulário do objetivo de carreira. Os limites dos campos repetem os do `GoalRequest` da API, para o
 * navegador avisar antes de enviar; a API confere de novo. Os valores ficam em estado local: um erro
 * da API nunca apaga o que foi digitado.
 */
export function GoalForm({ initial, onSaved, onCancel }: { initial: Goal | null; onSaved: (goal: Goal) => void; onCancel?: () => void }) {
  const [targetRole, setTargetRole] = useState(initial?.targetRole ?? "");
  const [targetLevel, setTargetLevel] = useState<GoalInput["targetLevel"]>((initial?.targetLevel as GoalInput["targetLevel"]) ?? "PLENO");
  const [track, setTrack] = useState<GoalInput["track"]>((initial?.track as GoalInput["track"]) ?? "BACKEND");
  const [deadline, setDeadline] = useState(initial?.deadline ?? "");
  const [hoursPerWeek, setHoursPerWeek] = useState(initial?.hoursPerWeek ?? 8);
  const [primaryStack, setPrimaryStack] = useState(initial?.primaryStack ?? "");
  const [wantsCertifications, setWantsCertifications] = useState(initial?.wantsCertifications ?? false);
  const [notes, setNotes] = useState(initial?.notes ?? "");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      onSaved(await saveGoal({ targetRole, targetLevel, track, deadline, hoursPerWeek, primaryStack, wantsCertifications, notes }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui salvar o objetivo. Seus dados continuam aqui; tente de novo.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="grid grid-cols-1 gap-5 md:grid-cols-2">
      <Field label="Cargo-alvo" className="md:col-span-2">
        <Input value={targetRole} onChange={(e) => setTargetRole(e.target.value)} required maxLength={120} placeholder="Ex.: Desenvolvedor Backend Java" />
      </Field>
      <Field label="Nível desejado">
        <Select value={targetLevel} onChange={(e) => setTargetLevel(e.target.value as GoalInput["targetLevel"])}>
          {LEVELS.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </Field>
      <Field label="Trilha">
        <Select value={track} onChange={(e) => setTrack(e.target.value as GoalInput["track"])}>
          {TRACKS.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </Field>
      <Field label="Prazo">
        <Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} required />
      </Field>
      <Field label="Horas por semana" hint="Entre 1 e 60. O plano cabe nesse orçamento.">
        <Input type="number" min={1} max={60} value={hoursPerWeek} onChange={(e) => setHoursPerWeek(Number(e.target.value))} required className="num" />
      </Field>
      <Field label="Stack principal" className="md:col-span-2">
        <Input value={primaryStack} onChange={(e) => setPrimaryStack(e.target.value)} maxLength={200} placeholder="Ex.: Java, Spring, PostgreSQL" />
      </Field>
      <label className="flex cursor-pointer items-center gap-2 text-body md:col-span-2">
        <input type="checkbox" checked={wantsCertifications} onChange={(e) => setWantsCertifications(e.target.checked)} className="size-4 accent-[var(--brand)]" />
        Quero certificações quando agregarem
      </label>
      <Field label="Observações" className="md:col-span-2">
        <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={2000} rows={3} />
      </Field>

      <div className="flex flex-wrap items-center gap-3 md:col-span-2">
        <Button type="submit" disabled={pending}>
          {pending ? "Salvando…" : "Salvar objetivo"}
        </Button>
        {onCancel ? (
          <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
            Cancelar
          </Button>
        ) : null}
        {error ? (
          <span role="alert" className="text-small text-destructive">
            {error}
          </span>
        ) : null}
      </div>
    </form>
  );
}
