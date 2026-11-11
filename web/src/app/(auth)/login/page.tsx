import { ShieldCheck } from "lucide-react";
import type { Metadata } from "next";

import { GithubMark } from "@/components/brand/github-mark";
import { Logo } from "@/components/brand/logo";
import { RedirectIfSignedIn } from "@/components/shell/redirect-if-signed-in";
import { brand } from "@/config/brand";
import { LOGIN_URL } from "@/lib/api/client";

export const metadata: Metadata = { title: "Entrar" };

export default function LoginPage() {
  return (
    <main className="grid min-h-dvh place-items-center px-4 py-10">
      <RedirectIfSignedIn />
      <div className="w-full max-w-sm">
        <Logo className="mb-10" />
        <h1 className="text-h1 font-semibold tracking-tight">{brand.tagline}</h1>
        <p className="mt-3 text-body text-muted-foreground">
          Conecte seu GitHub e receba um plano de carreira que mostra, para cada nota, a evidência que a sustenta.
        </p>

        <a
          href={LOGIN_URL}
          className="mt-8 flex h-11 items-center justify-center gap-2 rounded-control bg-primary px-4 font-medium text-primary-foreground outline-none transition-colors hover:bg-primary/90 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
        >
          <GithubMark className="size-5" />
          Entrar com GitHub
        </a>

        <div className="mt-8 space-y-3 rounded-card border border-border bg-surface p-4 text-small text-muted-foreground">
          <p className="flex items-center gap-2 font-medium text-foreground">
            <ShieldCheck className="size-4 text-brand" strokeWidth={1.5} aria-hidden />
            O que pedimos ao GitHub
          </p>
          <p>
            Só leitura do seu perfil e e-mail (<code className="font-mono text-micro">read:user</code>,{" "}
            <code className="font-mono text-micro">user:email</code>). Não escrevemos nada na sua conta.
          </p>
          <p>Você pode exportar todos os seus dados ou excluir a conta quando quiser, em Perfil › Meus dados.</p>
        </div>
      </div>
    </main>
  );
}
