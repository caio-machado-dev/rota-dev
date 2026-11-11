"use client";

import { Trash2 } from "lucide-react";
import { useSWRConfig } from "swr";

import { EmptyState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/components/ui/toast";
import { deleteSession, type Now } from "@/lib/api/client";
import { keys } from "@/lib/api/hooks";
import { duration, KIND_LABEL } from "@/lib/format";

/** Sessões registradas hoje, com opção de apagar. */
export function SessionList({ now }: { now: Now }) {
  const { mutate } = useSWRConfig();
  const toast = useToast();

  async function remove(id: string, topic: string) {
    try {
      await deleteSession(id);
      await Promise.all([mutate(keys.now), mutate(keys.readiness)]);
      toast.success(`Sessão “${topic}” apagada.`);
    } catch {
      toast.error("Não consegui apagar a sessão. Tente de novo.");
    }
  }

  return (
    <Card aria-labelledby="sessoes">
      <CardHeader>
        <div>
          <CardTitle id="sessoes">Sessões de hoje</CardTitle>
          <CardDescription>{now.minutesToday > 0 ? `${duration(now.minutesToday)} registrados` : "Nada registrado ainda"}</CardDescription>
        </div>
      </CardHeader>
      {now.sessionsToday.length === 0 ? (
        <EmptyState title="Você ainda não registrou sessões hoje" description="Use “Registrar sessão” (ou a tecla n) quando terminar de estudar." className="py-6" />
      ) : (
        <ul className="divide-y divide-border">
          {now.sessionsToday.map((s) => (
            <li key={s.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
              <div className="min-w-0 flex-1">
                <p className="text-body font-medium">{s.topic}</p>
                {s.learning ? <p className="mt-0.5 text-small text-muted-foreground">{s.learning}</p> : null}
              </div>
              <Badge tone="neutral">{KIND_LABEL[s.kind] ?? s.kind}</Badge>
              <span className="num w-14 text-right text-small text-muted-foreground">{duration(s.minutes)}</span>
              <Button variant="ghost" size="icon-sm" aria-label={`Apagar sessão ${s.topic}`} onClick={() => void remove(s.id, s.topic)}>
                <Trash2 strokeWidth={1.5} aria-hidden />
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
