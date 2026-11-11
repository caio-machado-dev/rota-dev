import { CircleAlert, Inbox, type LucideIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Estado vazio: diz o que falta e oferece a ação útil seguinte (um botão ou um link). */
export function EmptyState({
  title,
  description,
  action,
  icon: Icon = Inbox,
  className,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
  icon?: LucideIcon;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col items-center gap-3 px-4 py-10 text-center", className)}>
      <Icon className="size-6 text-muted-foreground" strokeWidth={1.5} aria-hidden />
      <div>
        <p className="font-medium">{title}</p>
        {description ? <p className="mt-1 max-w-sm text-secondary text-muted-foreground">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}

/** Estado de erro: contexto compreensível e "tentar de novo". Os dados já digitados ficam com quem chama. */
export function ErrorState({
  message,
  onRetry,
  className,
}: {
  message: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div role="alert" className={cn("flex items-start gap-3 rounded-card border border-destructive/30 bg-destructive/10 p-4", className)}>
      <CircleAlert className="mt-0.5 size-4 shrink-0 text-destructive" strokeWidth={1.5} aria-hidden />
      <div className="flex-1">
        <p className="text-secondary">{message}</p>
        {onRetry ? (
          <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>
            Tentar de novo
          </Button>
        ) : null}
      </div>
    </div>
  );
}
