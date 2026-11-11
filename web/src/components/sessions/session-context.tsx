"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";

import type { SessionInput } from "@/lib/api/client";

/** Valores iniciais para o formulário (ex.: ao registrar a partir de uma tarefa do plano). */
export type SessionPrefill = Partial<Pick<SessionInput, "kind" | "minutes" | "topic">>;

type SessionUi = {
  open: boolean;
  prefill: SessionPrefill | null;
  openSession: (prefill?: SessionPrefill) => void;
  setOpen: (open: boolean) => void;
};

const SessionContext = createContext<SessionUi | null>(null);

/** Estado de abertura do registro de sessão, para qualquer parte da interface abrir (botão, atalho `n`, paleta). */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [prefill, setPrefill] = useState<SessionPrefill | null>(null);

  const openSession = useCallback((next?: SessionPrefill) => {
    setPrefill(next ?? null);
    setOpen(true);
  }, []);

  const value = useMemo(() => ({ open, prefill, openSession, setOpen }), [open, prefill, openSession]);
  return <SessionContext value={value}>{children}</SessionContext>;
}

export function useSessionUi(): SessionUi {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSessionUi precisa estar dentro de <SessionProvider>.");
  return ctx;
}
