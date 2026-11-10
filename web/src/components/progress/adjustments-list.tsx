"use client";

import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { ApiError, decideAdjustment, listAdjustments, type Adjustment } from "@/lib/api/client";

const STATUS: Record<string, string> = { ACCEPTED: "aceito: nova versão do plano", REJECTED: "recusado" };
const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

/**
 * Reajustes (glossário) propostos nos check-ins. A pessoa aceita (gera uma versão nova do plano, com o
 * motivo) ou recusa. Nada muda no plano sem essa decisão.
 */
export function AdjustmentsList() {
  const [items, setItems] = useState<Adjustment[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setItems(await listAdjustments());
    } catch {
      setItems([]);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  if (items === null || items.length === 0) return null;

  const decide = async (id: string, decision: "accept" | "reject") => {
    setError(null);
    try {
      await decideAdjustment(id, decision);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não consegui registrar a decisão.");
    }
  };

  return (
    <section aria-labelledby="reajustes">
      <h2 id="reajustes" className="text-sm font-semibold">
        Reajustes do plano
      </h2>
      <ul className="mt-3 flex flex-col gap-3">
        {items.map((a) => (
          <li key={a.id} className="rounded-lg border border-border p-3 text-sm">
            <p className="font-mono text-xs text-muted-foreground">Check-in de {shortDate.format(new Date(`${a.weekStart}T12:00:00`))}</p>
            <p className="mt-1">{a.reason}</p>
            {a.status === "PROPOSED" ? (
              <div className="mt-3 flex gap-2">
                <Button size="sm" onClick={() => decide(a.id, "accept")}>
                  Aceitar e refazer o plano
                </Button>
                <Button size="sm" variant="outline" onClick={() => decide(a.id, "reject")}>
                  Manter o plano
                </Button>
              </div>
            ) : (
              <p className="mt-2 text-xs text-muted-foreground">{STATUS[a.status] ?? a.status}</p>
            )}
          </li>
        ))}
      </ul>
      {error ? (
        <p role="alert" className="mt-2 text-sm text-destructive">
          {error}
        </p>
      ) : null}
    </section>
  );
}
