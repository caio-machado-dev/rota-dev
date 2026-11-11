import { render } from "@testing-library/react";
import { SWRConfig } from "swr";
import { vi } from "vitest";

import { SessionProvider } from "@/components/sessions/session-context";
import { ToastProvider } from "@/components/ui/toast";

export type Call = { method: string; path: string; body: unknown };

/**
 * Simula a API: `routes` mapeia caminho → resposta JSON (ou `null` para 404). Escritas (POST/PUT/DELETE)
 * são registradas em `calls`; `onWrite` define a resposta delas (padrão: 200 com `{}`).
 */
export function stubApi(routes: Record<string, unknown>, onWrite?: (call: Call) => Response) {
  const calls: Call[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      const method = (init?.method ?? "GET").toUpperCase();
      if (method !== "GET") {
        const call = { method, path, body: typeof init?.body === "string" ? JSON.parse(init.body) : init?.body };
        calls.push(call);
        return onWrite ? onWrite(call) : Response.json({});
      }
      const value = routes[path];
      return value === undefined || value === null ? new Response(null, { status: 404 }) : Response.json(value);
    }),
  );
  return calls;
}

/** Renderiza com cache SWR isolado por teste e os provedores que as telas esperam. */
export function renderScreen(ui: React.ReactElement) {
  return render(
    <SWRConfig value={{ provider: () => new Map(), dedupingInterval: 0 }}>
      <ToastProvider>
        <SessionProvider>{ui}</SessionProvider>
      </ToastProvider>
    </SWRConfig>,
  );
}
