import { Loader2 } from "lucide-react";

import { ErrorState } from "./states";

/**
 * Estado de um trabalho assíncrono (diagnóstico, plano): em andamento ou falhou. O progresso é o que a API
 * sabe dizer de verdade (`PENDING`); não há etapas nem porcentagem inventadas.
 */
export function JobBanner({
  status,
  runningLabel,
  failedMessage,
  onRetry,
}: {
  status: string | undefined;
  runningLabel: string;
  failedMessage: string;
  onRetry: () => void;
}) {
  if (status === "PENDING") {
    return (
      <div role="status" className="flex items-center gap-3 rounded-card border border-brand/30 bg-brand/10 p-4 text-small">
        <Loader2 className="size-4 animate-spin text-brand" strokeWidth={1.5} aria-hidden />
        <span>
          {runningLabel} Pode sair desta tela: a página se atualiza sozinha quando terminar.
        </span>
      </div>
    );
  }
  if (status === "FAILED") return <ErrorState message={failedMessage} onRetry={onRetry} />;
  return null;
}
