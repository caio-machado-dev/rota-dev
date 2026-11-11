"use client";

import { CircleAlert, CircleCheck } from "lucide-react";
import { createContext, useCallback, useContext, useMemo, useRef, useState } from "react";

import { cn } from "@/lib/utils";

type Toast = { id: number; message: string; tone: "success" | "error" };
type ToastApi = { success: (message: string) => void; error: (message: string) => void };

const ToastContext = createContext<ToastApi | null>(null);

const DURATION_MS = 4500;

/** Feedback discreto de ação concluída ou falha. A região é `aria-live`, então leitores de tela anunciam. */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const next = useRef(1);

  const push = useCallback((tone: Toast["tone"], message: string) => {
    const id = next.current++;
    setToasts((current) => [...current, { id, message, tone }]);
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), DURATION_MS);
  }, []);

  const api = useMemo<ToastApi>(() => ({ success: (m) => push("success", m), error: (m) => push("error", m) }), [push]);

  return (
    <ToastContext value={api}>
      {children}
      <div
        role="status"
        aria-live="polite"
        className="pointer-events-none fixed inset-x-4 bottom-20 z-[60] flex flex-col items-end gap-2 md:bottom-6 md:right-6 md:left-auto"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className={cn(
              "pointer-events-auto flex max-w-sm items-start gap-2 rounded-card border bg-popover px-3.5 py-2.5 text-small shadow-raised animate-in fade-in slide-in-from-bottom-2 duration-200",
              t.tone === "success" ? "border-success/40" : "border-destructive/40",
            )}
          >
            {t.tone === "success" ? (
              <CircleCheck className="mt-0.5 size-4 shrink-0 text-success" strokeWidth={1.5} aria-hidden />
            ) : (
              <CircleAlert className="mt-0.5 size-4 shrink-0 text-destructive" strokeWidth={1.5} aria-hidden />
            )}
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast precisa estar dentro de <ToastProvider>.");
  return ctx;
}
