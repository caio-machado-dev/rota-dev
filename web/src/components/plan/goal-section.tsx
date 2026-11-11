"use client";

import { useState } from "react";
import { useSWRConfig } from "swr";

import { ErrorState } from "@/components/system/states";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { keys, useGoal } from "@/lib/api/hooks";

import { CareerGoalCard } from "./career-goal-card";
import { GoalForm } from "./goal-form";

/** Aba "Objetivo": formulário quando não há objetivo (ou ao editar); resumo compacto depois de salvar. */
export function GoalSection() {
  const { data: goal, error, isLoading, mutate: reload } = useGoal();
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const [editing, setEditing] = useState(false);

  if (isLoading) return <Skeleton className="h-72 max-w-3xl" />;
  if (error) return <ErrorState message="Não consegui carregar o seu objetivo." onRetry={() => void reload()} />;

  if (goal && !editing) {
    return (
      <div className="max-w-3xl">
        <CareerGoalCard goal={goal} onEdit={() => setEditing(true)} />
      </div>
    );
  }

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <div>
          <CardTitle>{goal ? "Editar objetivo" : "Aonde você quer chegar?"}</CardTitle>
          <CardDescription>O cargo, o prazo e as horas semanais definem o diagnóstico e o plano.</CardDescription>
        </div>
      </CardHeader>
      <GoalForm
        initial={goal ?? null}
        onCancel={goal ? () => setEditing(false) : undefined}
        onSaved={async (saved) => {
          await mutate(keys.goal, saved, { revalidate: false });
          await Promise.all([mutate(keys.now), mutate(keys.readiness)]);
          setEditing(false);
          toast.success("Objetivo salvo.");
        }}
      />
    </Card>
  );
}
