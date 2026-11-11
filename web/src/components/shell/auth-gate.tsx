"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { ErrorState } from "@/components/system/states";
import { Skeleton } from "@/components/ui/skeleton";
import { useMe } from "@/lib/api/hooks";

/**
 * Guarda das telas internas: sem sessão (401), manda para `/login`. Fica num lugar só, em vez de
 * cada painel tratar o 401 por conta própria. Mostra esqueleto enquanto confirma a sessão.
 */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const { data: me, error, isLoading, mutate } = useMe();
  const router = useRouter();

  useEffect(() => {
    if (me === null) router.replace("/login");
  }, [me, router]);

  if (error) return <ErrorState message="Não consegui falar com o servidor. Verifique a conexão." onRetry={() => void mutate()} />;
  if (isLoading || me === null) {
    return (
      <div className="space-y-4" aria-busy="true" aria-label="Carregando">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }
  return <>{children}</>;
}
