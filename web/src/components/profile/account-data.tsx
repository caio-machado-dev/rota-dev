"use client";

import { Download } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { ApiError, deleteAccount, EXPORT_URL } from "@/lib/api/client";

/**
 * Seus dados (LGPD): baixar tudo em JSON e excluir a conta. A exclusão pede o login do GitHub digitado,
 * para não acontecer por um clique sem querer.
 */
export function AccountData({
  // Recarga completa de propósito: sem conta, o menu do usuário e todo o estado do cliente recomeçam do zero.
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  onDeleted = () => window.location.assign("/"),
}: {
  onDeleted?: () => void;
}) {
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  return (
    <section aria-labelledby="seus-dados" className="flex flex-col gap-4">
      <h2 id="seus-dados" className="text-sm font-semibold">
        Seus dados
      </h2>
      <p className="text-sm text-muted-foreground">
        Tudo o que o Rota Dev guarda sobre você, num arquivo JSON. Tokens e caminhos internos ficam de fora.
      </p>
      <a
        href={EXPORT_URL}
        download
        className="inline-flex w-fit items-center gap-2 rounded-md border border-border px-3 py-1.5 text-sm hover:bg-muted"
      >
        <Download className="size-4" aria-hidden />
        Baixar meus dados
      </a>

      <form
        className="mt-4 flex max-w-md flex-col gap-2 rounded-lg border border-destructive/40 p-4"
        onSubmit={async (event) => {
          event.preventDefault();
          setPending(true);
          setError(null);
          try {
            await deleteAccount(confirm.trim());
            onDeleted();
          } catch (e) {
            setError(e instanceof ApiError ? e.message : "Não consegui excluir a conta.");
          } finally {
            setPending(false);
          }
        }}
      >
        <p className="text-sm font-medium text-destructive">Excluir a conta</p>
        <p className="text-sm text-muted-foreground">
          Apaga de vez o perfil, os arquivos enviados, o plano e todo o histórico. Não dá para desfazer.
        </p>
        <label className="text-sm">
          Digite o seu login do GitHub para confirmar
          <input
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            autoComplete="off"
            className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </label>
        <Button type="submit" variant="destructive" size="sm" disabled={pending || confirm.trim() === ""} className="self-start">
          Excluir minha conta
        </Button>
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}
      </form>
    </section>
  );
}
