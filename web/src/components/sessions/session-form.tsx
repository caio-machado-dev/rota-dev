"use client";

import { Pause, Play, RotateCcw } from "lucide-react";
import { useState } from "react";
import { useSWRConfig } from "swr";

import { Button } from "@/components/ui/button";
import { Field, Input, Textarea } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { ApiError, logSession, type SessionInput } from "@/lib/api/client";
import { keys } from "@/lib/api/hooks";
import { duration, KIND_LABEL, SESSION_KINDS } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { SessionPrefill } from "./session-context";
import { clock, useStopwatch } from "./use-stopwatch";

const PRESETS = [15, 30, 45, 60, 90];
const MIN_MINUTES = 5;
const MAX_MINUTES = 600;

/**
 * Formulário de registro de sessão: manual (com presets de duração) ou com cronômetro.
 * Os campos ficam em estado local, então um erro da API nunca apaga o que a pessoa digitou.
 */
export function SessionForm({ prefill, onDone }: { prefill?: SessionPrefill | null; onDone: () => void }) {
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const watch = useStopwatch();

  const [mode, setMode] = useState<"manual" | "timer">("manual");
  const [topic, setTopic] = useState(prefill?.topic ?? "");
  const [kind, setKind] = useState<SessionInput["kind"]>(prefill?.kind ?? "STUDY");
  const [minutes, setMinutes] = useState(prefill?.minutes ?? 30);
  const [day, setDay] = useState("");
  const [learning, setLearning] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function stopTimer() {
    watch.pause();
    setMinutes(Math.min(MAX_MINUTES, Math.max(MIN_MINUTES, Math.ceil(watch.elapsedSeconds / 60))));
    setMode("manual");
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      await logSession({ topic: topic.trim(), kind, minutes, learning: learning.trim(), ...(day ? { day } : {}) });
      await Promise.all([mutate(keys.now), mutate(keys.readiness), mutate(keys.checkin)]);
      toast.success(`Sessão registrada: ${duration(minutes)} de ${KIND_LABEL[kind].toLowerCase()}.`);
      watch.reset();
      onDone();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui registrar a sessão. Seus dados continuam aqui; tente de novo.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div role="radiogroup" aria-label="Como registrar" className="grid grid-cols-2 gap-1 rounded-control border border-border p-0.5 text-small">
        {(["manual", "timer"] as const).map((value) => (
          <button
            key={value}
            type="button"
            role="radio"
            aria-checked={mode === value}
            onClick={() => setMode(value)}
            className={cn("rounded-[4px] py-1.5 font-medium text-muted-foreground transition-colors", mode === value && "bg-accent text-foreground")}
          >
            {value === "manual" ? "Manual" : "Cronômetro"}
          </button>
        ))}
      </div>

      <Field label="Tema">
        <Input value={topic} onChange={(e) => setTopic(e.target.value)} required maxLength={200} placeholder="Ex.: transações no Spring" />
      </Field>

      <fieldset>
        <legend className="mb-1.5 text-small font-medium">Tipo</legend>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {SESSION_KINDS.map((value) => (
            <label
              key={value}
              className={cn(
                "cursor-pointer rounded-control border border-input px-2 py-1.5 text-center text-small has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring",
                kind === value && "border-brand bg-brand/10 text-foreground",
              )}
            >
              <input type="radio" name="kind" value={value} checked={kind === value} onChange={() => setKind(value)} className="sr-only" />
              {KIND_LABEL[value]}
            </label>
          ))}
        </div>
      </fieldset>

      {mode === "timer" ? (
        <div className="rounded-card border border-border bg-surface p-4 text-center">
          <p className="num font-mono text-h1" aria-live="off">
            {clock(watch.elapsedSeconds)}
          </p>
          <div className="mt-3 flex justify-center gap-2">
            {watch.running ? (
              <Button type="button" variant="outline" onClick={watch.pause}>
                <Pause aria-hidden /> Pausar
              </Button>
            ) : (
              <Button type="button" onClick={watch.start}>
                <Play aria-hidden /> {watch.elapsedSeconds > 0 ? "Continuar" : "Iniciar"}
              </Button>
            )}
            <Button type="button" variant="outline" onClick={watch.reset} aria-label="Zerar cronômetro" disabled={watch.elapsedSeconds === 0}>
              <RotateCcw aria-hidden />
            </Button>
          </div>
          <Button type="button" variant="secondary" className="mt-3" disabled={watch.elapsedSeconds === 0} onClick={stopTimer}>
            Usar este tempo ({duration(Math.max(MIN_MINUTES, Math.ceil(watch.elapsedSeconds / 60)))})
          </Button>
        </div>
      ) : (
        <div>
          <p className="mb-1.5 text-small font-medium">Duração</p>
          <div className="flex flex-wrap gap-2">
            {PRESETS.map((preset) => (
              <button
                key={preset}
                type="button"
                aria-pressed={minutes === preset}
                onClick={() => setMinutes(preset)}
                className={cn(
                  "num rounded-full border border-input px-3 py-1 text-small transition-colors hover:bg-accent",
                  minutes === preset && "border-brand bg-brand/10",
                )}
              >
                {preset} min
              </button>
            ))}
          </div>
          <Field label="Minutos" className="mt-3">
            <Input type="number" min={MIN_MINUTES} max={MAX_MINUTES} required value={minutes} onChange={(e) => setMinutes(Number(e.target.value))} className="num w-28" />
          </Field>
        </div>
      )}

      <Field label="Data" hint="Em branco, vale hoje no seu fuso.">
        <Input type="date" value={day} onChange={(e) => setDay(e.target.value)} className="w-44" />
      </Field>

      <Field label="O que aprendi" hint="Uma linha. Vira a evidência do seu estudo.">
        <Textarea value={learning} onChange={(e) => setLearning(e.target.value)} maxLength={500} rows={2} />
      </Field>

      {error ? (
        <p role="alert" className="text-small text-destructive">
          {error}
        </p>
      ) : null}

      <Button type="submit" size="lg" disabled={pending || !topic.trim() || minutes < MIN_MINUTES}>
        {pending ? "Registrando…" : "Registrar sessão"}
      </Button>
    </form>
  );
}
