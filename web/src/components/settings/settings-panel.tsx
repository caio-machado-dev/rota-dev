"use client";

import Link from "next/link";

import { ThemeToggle } from "@/components/theme/theme-toggle";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useMe } from "@/lib/api/hooks";

/** Preferências que existem hoje: tema (guardado no navegador) e um resumo da conta com atalho para os dados. */
export function SettingsPanel() {
  const { data: me } = useMe();
  return (
    <div className="grid max-w-3xl gap-6">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Tema</CardTitle>
            <CardDescription>Escuro é o padrão. “Sistema” acompanha o seu dispositivo. Fica salvo neste navegador.</CardDescription>
          </div>
          <ThemeToggle />
        </CardHeader>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Conta</CardTitle>
            <CardDescription>Entrada pelo GitHub.</CardDescription>
          </div>
        </CardHeader>
        {me ? (
          <dl className="grid grid-cols-[8rem_1fr] gap-y-2 text-small">
            <dt className="text-muted-foreground">Login</dt>
            <dd className="font-mono">{me.login}</dd>
            <dt className="text-muted-foreground">E-mail</dt>
            <dd>{me.email ?? "Não informado"}</dd>
            <dt className="text-muted-foreground">Fuso horário</dt>
            <dd>{me.timeZone}</dd>
            <dt className="text-muted-foreground">Plano</dt>
            <dd>{me.tier}</dd>
          </dl>
        ) : null}
        <p className="mt-4 text-small text-muted-foreground">
          Exportar dados e excluir a conta ficam em{" "}
          <Link href="/perfil" className="text-brand hover:underline">
            Perfil
          </Link>
          .
        </p>
      </Card>
    </div>
  );
}
