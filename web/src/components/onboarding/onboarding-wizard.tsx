"use client";

import { ArrowLeft, ArrowRight, CircleCheck, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useSWRConfig } from "swr";

import { GoalForm } from "@/components/plan/goal-form";
import { GithubCard, UploadCard } from "@/components/profile/sources-section";
import { ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError, requestDiagnosis } from "@/lib/api/client";
import { keys, useGoal, useLatestDiagnosis, useMe, useProfile, useSources, useUploads } from "@/lib/api/hooks";
import { latestBySource, skillRows, SOURCE_LABEL } from "@/lib/domain/profile";

import { Stepper } from "./stepper";

const STEPS = ["Conectar fontes", "Revisar perfil", "Definir objetivo", "Gerar diagnóstico"];

function SourcesStep() {
  const { data: sources } = useSources();
  const { data: uploads } = useUploads();
  return (
    <div className="space-y-4">
      <p className="text-body text-muted-foreground">O GitHub já está conectado pelo login. Se quiser, envie também o currículo e o LinkedIn: mais fontes, mais evidências.</p>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="md:col-span-2">
          <GithubCard source={sources?.find((s) => s.type === "GITHUB")} />
        </div>
        <UploadCard kind="RESUME" uploads={uploads ?? []} />
        <UploadCard kind="LINKEDIN" uploads={uploads ?? []} />
      </div>
    </div>
  );
}

function ReviewStep() {
  const { data: profile, mutate } = useProfile();
  const snapshots = profile?.snapshots ?? [];

  // A leitura roda em segundo plano: enquanto não há nada, refaz a busca a cada 3 s.
  useEffect(() => {
    if (snapshots.length > 0) return;
    const id = setInterval(() => void mutate(), 3000);
    return () => clearInterval(id);
  }, [snapshots.length, mutate]);

  if (!profile) return <Skeleton className="h-48" />;
  if (snapshots.length === 0) {
    return (
      <Card role="status" className="flex items-center gap-3">
        <Loader2 className="size-5 animate-spin text-brand" strokeWidth={1.5} aria-hidden />
        <p className="text-body">Ainda estamos lendo as suas fontes. Esta tela se atualiza sozinha quando houver resultado.</p>
      </Card>
    );
  }

  const rows = skillRows(snapshots).sort((a, b) => b.weight - a.weight);
  const evidenceCount = latestBySource(snapshots).reduce((sum, s) => sum + s.evidences.length, 0);
  return (
    <div className="space-y-4">
      <p className="text-body text-muted-foreground">
        Isto é o que encontramos: {rows.length} habilidades e {evidenceCount} evidências. Você pode ajustar depois em Perfil.
      </p>
      <Card>
        <ul className="flex flex-wrap gap-2" aria-label="Habilidades encontradas">
          {rows.slice(0, 24).map((r) => (
            <li key={r.key}>
              <Badge tone={r.confidence === "HIGH" ? "success" : "neutral"} title={`Fonte: ${SOURCE_LABEL[r.source] ?? r.source}`}>
                {r.name}
              </Badge>
            </li>
          ))}
        </ul>
        {rows.length > 24 ? <p className="mt-3 text-small text-muted-foreground">e mais {rows.length - 24}…</p> : null}
      </Card>
    </div>
  );
}

function GoalStep({ onSaved }: { onSaved: () => void }) {
  const { data: goal, mutate } = useGoal();
  const { mutate: globalMutate } = useSWRConfig();
  if (goal === undefined) return <Skeleton className="h-72" />;
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Aonde você quer chegar?</CardTitle>
          <CardDescription>O cargo, o prazo e as horas semanais definem o diagnóstico e o plano.</CardDescription>
        </div>
      </CardHeader>
      <GoalForm
        initial={goal}
        onSaved={async (saved) => {
          await mutate(saved, { revalidate: false });
          await globalMutate(keys.now);
          onSaved();
        }}
      />
    </Card>
  );
}

function DiagnosisStep() {
  const router = useRouter();
  const { mutate } = useSWRConfig();
  const { data: goal } = useGoal();
  const { data: sources } = useSources();
  const { data: profile } = useProfile();
  const { data: diagnosis, mutate: reload } = useLatestDiagnosis();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function generate() {
    setPending(true);
    setError(null);
    try {
      await requestDiagnosis();
      await mutate(keys.diagnosis);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui pedir o diagnóstico.");
    } finally {
      setPending(false);
    }
  }

  const running = diagnosis?.status === "PENDING";
  const ready = diagnosis?.status === "READY";
  const read = latestBySource(profile?.snapshots ?? []);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Tudo pronto para o diagnóstico</CardTitle>
            <CardDescription>Ele compara o que você já construiu com o que o cargo-alvo exige, com as evidências de cada nota.</CardDescription>
          </div>
        </CardHeader>
        <ul className="space-y-2 text-body">
          <li className="flex items-center gap-2">
            <CircleCheck className="size-4 text-success" strokeWidth={1.5} aria-hidden /> Cargo-alvo: {goal?.targetRole ?? "—"}
          </li>
          <li className="flex items-center gap-2">
            <CircleCheck className="size-4 text-success" strokeWidth={1.5} aria-hidden />
            Fontes lidas: {read.length > 0 ? read.map((s) => SOURCE_LABEL[s.sourceType] ?? s.sourceType).join(", ") : sources?.length ? "GitHub (leitura em andamento)" : "nenhuma ainda"}
          </li>
        </ul>
      </Card>

      {running ? (
        <div role="status" className="flex items-center gap-3 rounded-card border border-brand/30 bg-brand/10 p-4 text-small">
          <Loader2 className="size-4 animate-spin text-brand" strokeWidth={1.5} aria-hidden />
          Gerando o diagnóstico. Pode levar alguns minutos; esta tela avisa quando terminar.
        </div>
      ) : null}
      {diagnosis?.status === "FAILED" ? <ErrorState message="Não foi possível gerar o diagnóstico. Tente de novo." onRetry={() => void generate()} /> : null}
      {error ? <ErrorState message={error} onRetry={() => void reload()} /> : null}

      <div className="flex flex-wrap gap-3">
        {ready ? (
          <Button size="lg" onClick={() => router.push("/plano/diagnostico")}>
            Ver o diagnóstico <ArrowRight aria-hidden />
          </Button>
        ) : (
          <Button size="lg" onClick={() => void generate()} disabled={pending || running || !goal}>
            {running ? "Gerando…" : "Gerar diagnóstico"}
          </Button>
        )}
      </div>
    </div>
  );
}

/**
 * Onboarding em quatro etapas: fontes → perfil encontrado → objetivo → diagnóstico. Não tem estado próprio no servidor:
 * cada etapa usa o que a API já guarda (fontes, perfil, objetivo, diagnóstico), então dá para sair e voltar.
 */
export function OnboardingWizard() {
  const router = useRouter();
  const { data: me, isLoading } = useMe();
  const { data: goal } = useGoal();
  const { data: sources } = useSources();
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (me === null) router.replace("/login");
  }, [me, router]);

  if (isLoading || !me) return <Skeleton className="mx-auto h-96 max-w-3xl" />;

  const hasGithub = Boolean(sources?.some((s) => s.type === "GITHUB"));
  const canContinue = step === 0 ? hasGithub : step === 1 ? true : step === 2 ? Boolean(goal) : false;

  return (
    <div className="mx-auto w-full max-w-3xl space-y-8">
      <Stepper steps={STEPS} current={step} />
      <h1 className="text-h1 font-semibold tracking-tight">{STEPS[step]}</h1>

      {step === 0 ? <SourcesStep /> : null}
      {step === 1 ? <ReviewStep /> : null}
      {step === 2 ? <GoalStep onSaved={() => setStep(3)} /> : null}
      {step === 3 ? <DiagnosisStep /> : null}

      <div className="flex items-center justify-between border-t border-border pt-5">
        <Button variant="ghost" onClick={() => (step === 0 ? router.push("/") : setStep(step - 1))}>
          {step === 0 ? "Pular por agora" : <><ArrowLeft aria-hidden /> Voltar</>}
        </Button>
        {step < 2 ? (
          <Button onClick={() => setStep(step + 1)} disabled={!canContinue}>
            Continuar <ArrowRight aria-hidden />
          </Button>
        ) : step === 2 && goal ? (
          <Button variant="outline" onClick={() => setStep(3)}>
            Já salvei, continuar <ArrowRight aria-hidden />
          </Button>
        ) : null}
      </div>
    </div>
  );
}
