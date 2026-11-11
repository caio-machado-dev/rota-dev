"use client";

import { Download, ShieldCheck } from "lucide-react";
import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/dialog";
import { Field, Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError, deleteAccount, EXPORT_URL } from "@/lib/api/client";
import { useMe } from "@/lib/api/hooks";

/**
 * Aba "Meus dados" (LGPD): informações da conta, exportação em JSON e exclusão. A exclusão exige duas
 * confirmações: abrir o diálogo e digitar o login do GitHub.
 */
export function AccountSection({
  // Recarga completa de propósito: sem conta, todo o estado do cliente recomeça do zero.
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  onDeleted = () => window.location.assign("/login"),
}: {
  onDeleted?: () => void;
}) {
  const { data: me } = useMe();
  const [open, setOpen] = useState(false);
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!me) return <Skeleton className="h-72" />;

  async function remove() {
    setBusy(true);
    setError(null);
    try {
      await deleteAccount(confirm.trim());
      onDeleted();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui excluir a conta.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-3xl space-y-6">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Conta</CardTitle>
            <CardDescription>Entrada pelo GitHub.</CardDescription>
          </div>
          <Badge tone="brand">{me.tier}</Badge>
        </CardHeader>
        <dl className="grid grid-cols-[9rem_1fr] gap-y-2 text-body">
          <dt className="text-muted-foreground">Nome</dt>
          <dd>{me.name ?? "—"}</dd>
          <dt className="text-muted-foreground">Login</dt>
          <dd className="font-mono">{me.login}</dd>
          <dt className="text-muted-foreground">E-mail</dt>
          <dd>{me.email ?? "Não informado"}</dd>
          <dt className="text-muted-foreground">Fuso horário</dt>
          <dd>{me.timeZone}</dd>
          <dt className="text-muted-foreground">Plano</dt>
          <dd>{me.tier}</dd>
        </dl>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Exportar meus dados</CardTitle>
            <CardDescription>Tudo o que o Rota Dev guarda sobre você, num arquivo JSON. Tokens e caminhos internos ficam de fora.</CardDescription>
          </div>
        </CardHeader>
        <a href={EXPORT_URL} download className="inline-flex h-8 w-fit items-center gap-2 rounded-lg border border-border px-3 text-small font-medium hover:bg-accent">
          <Download className="size-4" strokeWidth={1.5} aria-hidden />
          Baixar meus dados
        </a>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldCheck className="size-5 text-brand" strokeWidth={1.5} aria-hidden />
            <CardTitle>Privacidade</CardTitle>
          </div>
        </CardHeader>
        <ul className="list-disc space-y-1 pl-5 text-small text-muted-foreground">
          <li>Pedimos ao GitHub só leitura do perfil e do e-mail.</li>
          <li>Os tokens ficam cifrados e nunca saem da API.</li>
          <li>Cada dado é seu: você exporta ou apaga tudo a qualquer momento.</li>
        </ul>
      </Card>

      <Card className="border-destructive/40">
        <CardHeader>
          <div>
            <CardTitle className="text-destructive">Excluir a conta</CardTitle>
            <CardDescription>Apaga de vez o perfil, os arquivos enviados, o plano e todo o histórico. Não dá para desfazer.</CardDescription>
          </div>
        </CardHeader>
        <Button variant="destructive" onClick={() => setOpen(true)}>
          Excluir minha conta
        </Button>
      </Card>

      <ConfirmDialog
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) {
            setConfirm("");
            setError(null);
          }
        }}
        title="Excluir a conta para sempre?"
        description="Isso apaga todos os seus dados e não pode ser desfeito. Baixe a exportação antes, se quiser guardar algo."
        confirmLabel="Excluir definitivamente"
        destructive
        busy={busy}
        confirmDisabled={confirm.trim() !== me.login}
        onConfirm={remove}
      >
        <Field label={`Digite ${me.login} para confirmar`}>
          <Input value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="off" />
        </Field>
        {error ? (
          <p role="alert" className="mt-2 text-small text-destructive">
            {error}
          </p>
        ) : null}
      </ConfirmDialog>
    </div>
  );
}
