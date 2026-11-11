"use client";

import { Check } from "lucide-react";
import { useState } from "react";
import { useSWRConfig } from "swr";

import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { ApiError, saveHabit, type Now } from "@/lib/api/client";
import { keys } from "@/lib/api/hooks";
import { cn } from "@/lib/utils";

const STEPS = [
  { name: "readDiff", label: "Li o diff inteiro" },
  { name: "explained", label: "Consigo explicar o que o código faz" },
  { name: "tested", label: "Escrevi ou executei um teste" },
] as const;
type StepName = (typeof STEPS)[number]["name"];

/**
 * Protocolo anti vibe coding: três verificações antes de aceitar código gerado por IA.
 * Cada marcação salva na hora (PUT idempotente do dia); a nota tem botão próprio.
 * O histórico e a sequência de dias não aparecem porque a API só devolve o dia de hoje.
 */
export function AntiVibe({ now }: { now: Now }) {
  const habit = now.habitToday;
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const [steps, setSteps] = useState<Record<StepName, boolean>>({
    readDiff: habit?.readDiff ?? false,
    explained: habit?.explained ?? false,
    tested: habit?.tested ?? false,
  });
  const [note, setNote] = useState(habit?.note ?? "");
  const [saving, setSaving] = useState(false);
  const done = STEPS.filter(({ name }) => steps[name]).length;

  async function persist(nextSteps: Record<StepName, boolean>, nextNote: string, okMessage?: string) {
    setSaving(true);
    try {
      await saveHabit({ day: now.today, ...nextSteps, note: nextNote });
      await Promise.all([mutate(keys.now), mutate(keys.readiness)]);
      if (okMessage) toast.success(okMessage);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Não consegui salvar o protocolo.");
      setSteps(steps); // desfaz a marcação que não foi salva
    } finally {
      setSaving(false);
    }
  }

  function toggle(name: StepName) {
    const next = { ...steps, [name]: !steps[name] };
    setSteps(next);
    void persist(next, note);
  }

  return (
    <Card aria-labelledby="protocolo">
      <CardHeader>
        <div>
          <CardTitle id="protocolo">Protocolo anti vibe coding</CardTitle>
          <CardDescription>Antes de aceitar código gerado por IA, prove que você o entende.</CardDescription>
        </div>
        <p className="num shrink-0 text-h3 font-semibold" aria-label={`${done} de 3 passos`}>
          {done}/3
        </p>
      </CardHeader>

      <div className="mb-4 grid grid-cols-3 gap-1.5" aria-hidden>
        {STEPS.map(({ name }) => (
          <span key={name} className={cn("h-1.5 rounded-full bg-muted transition-colors duration-200", steps[name] && "bg-brand")} />
        ))}
      </div>

      <ul className="space-y-1">
        {STEPS.map(({ name, label }) => (
          <li key={name}>
            <label className="flex min-h-11 cursor-pointer items-center gap-3 rounded-control px-2 hover:bg-accent/50 has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring">
              <input type="checkbox" checked={steps[name]} onChange={() => toggle(name)} disabled={saving} className="peer sr-only" />
              <span
                aria-hidden
                className="grid size-5 shrink-0 place-items-center rounded-[5px] border border-input text-brand-foreground transition-colors peer-checked:border-brand peer-checked:bg-brand"
              >
                {steps[name] ? <Check className="size-3.5" strokeWidth={2.5} /> : null}
              </span>
              <span className={cn("text-body", steps[name] && "text-muted-foreground")}>{label}</span>
            </label>
          </li>
        ))}
      </ul>

      <div className="mt-4">
        <label htmlFor="habit-note" className="mb-1.5 block text-small font-medium">
          Nota do dia
        </label>
        <Textarea id="habit-note" value={note} onChange={(e) => setNote(e.target.value)} maxLength={1000} rows={2} placeholder="O que esse código me ensinou?" />
        <div className="mt-2 flex items-center gap-3">
          <Button variant="outline" size="sm" disabled={saving || note === (habit?.note ?? "")} onClick={() => void persist(steps, note, "Nota salva.")}>
            Salvar nota
          </Button>
          {habit?.complete ? <span className="text-small text-success">Dia completo.</span> : null}
        </div>
      </div>
    </Card>
  );
}
