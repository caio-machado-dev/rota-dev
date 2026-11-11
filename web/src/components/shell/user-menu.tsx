"use client";

import { LogIn, LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { getMe, LOGIN_URL, logout, type Me } from "@/lib/api/client";

type State = { status: "loading" } | { status: "anonymous" } | { status: "signed-in"; me: Me };

/**
 * Mostra quem está logado ou o botão de entrar. Busca `/api/me` no navegador, depois que a página
 * carrega: o cookie de sessão vai junto sozinho, porque a API está na mesma origem.
 */
export function UserMenu({ compact = false }: { compact?: boolean }) {
  const [state, setState] = useState<State>({ status: "loading" });
  const router = useRouter();

  useEffect(() => {
    let active = true;
    getMe()
      .then((me) => active && setState(me ? { status: "signed-in", me } : { status: "anonymous" }))
      .catch(() => active && setState({ status: "anonymous" }));
    return () => {
      active = false;
    };
  }, []);

  if (state.status === "loading") {
    return <div className="h-9" aria-hidden />;
  }

  if (state.status === "anonymous") {
    return (
      <a
        href={LOGIN_URL}
        aria-label="Entrar com GitHub"
        className="flex items-center justify-center gap-2 rounded-control px-3 py-2 text-small font-medium text-brand hover:bg-sidebar-accent"
      >
        <LogIn className="size-4" aria-hidden />
        {compact ? null : "Entrar com GitHub"}
      </a>
    );
  }

  const { me } = state;
  return (
    <div className={compact ? "flex flex-col items-center gap-2 py-1" : "flex items-center gap-3 px-3 py-2"}>
      {me.avatarUrl ? (
        // eslint-disable-next-line @next/next/no-img-element -- avatar externo e pequeno, sem otimização.
        <img src={me.avatarUrl} alt="" className="size-7 rounded-full" />
      ) : null}
      {compact ? null : <span className="min-w-0 flex-1 truncate text-small">{me.name || me.login}</span>}
      <button
        type="button"
        onClick={async () => {
          await logout();
          setState({ status: "anonymous" });
          router.push("/");
        }}
        className="rounded-md p-1.5 text-muted-foreground hover:bg-sidebar-accent hover:text-foreground"
        aria-label="Sair"
      >
        <LogOut className="size-4" aria-hidden />
      </button>
    </div>
  );
}
