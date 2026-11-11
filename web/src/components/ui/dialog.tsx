"use client";

import { Dialog as DialogPrimitive } from "@base-ui/react/dialog";
import { X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const Dialog = DialogPrimitive.Root;
const DialogTrigger = DialogPrimitive.Trigger;
const DialogClose = DialogPrimitive.Close;

const placement = {
  /** Modal centralizado (desktop). */
  center:
    "left-1/2 top-1/2 w-[min(92vw,32rem)] -translate-x-1/2 -translate-y-1/2 rounded-modal data-[starting-style]:scale-95 data-[ending-style]:scale-95",
  /** Painel lateral à direita (desktop). */
  right:
    "right-0 top-0 h-dvh w-[min(100vw,28rem)] rounded-l-modal data-[starting-style]:translate-x-8 data-[ending-style]:translate-x-8",
  /** Bottom sheet (celular). */
  bottom:
    "inset-x-0 bottom-0 max-h-[90dvh] rounded-t-modal pb-[max(1.25rem,env(safe-area-inset-bottom))] data-[starting-style]:translate-y-8 data-[ending-style]:translate-y-8",
} as const;

/**
 * Conteúdo de modal ou sheet. Foco preso, Esc fecha e o foco volta ao gatilho (base-ui cuida disso).
 * `title` é obrigatório: todo diálogo precisa de nome para leitores de tela.
 */
function DialogContent({
  title,
  description,
  placement: place = "center",
  className,
  children,
}: {
  title: string;
  description?: string;
  placement?: keyof typeof placement;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <DialogPrimitive.Portal>
      <DialogPrimitive.Backdrop className="fixed inset-0 z-50 bg-black/60 transition-opacity duration-200 data-[starting-style]:opacity-0 data-[ending-style]:opacity-0" />
      <DialogPrimitive.Popup
        className={cn(
          "fixed z-50 overflow-y-auto border border-border bg-popover p-5 text-popover-foreground shadow-overlay outline-none transition duration-200 ease-out-soft data-[starting-style]:opacity-0 data-[ending-style]:opacity-0",
          placement[place],
          className,
        )}
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <div>
            <DialogPrimitive.Title className="text-h3 font-semibold tracking-tight">{title}</DialogPrimitive.Title>
            {description ? (
              <DialogPrimitive.Description className="mt-1 text-small text-muted-foreground">{description}</DialogPrimitive.Description>
            ) : null}
          </div>
          <DialogPrimitive.Close render={<Button variant="ghost" size="icon-sm" aria-label="Fechar" />}>
            <X strokeWidth={1.5} aria-hidden />
          </DialogPrimitive.Close>
        </div>
        {children}
      </DialogPrimitive.Popup>
    </DialogPrimitive.Portal>
  );
}

/**
 * Confirmação explícita para ação destrutiva ou difícil de desfazer. Quem chama decide o que acontece
 * em `onConfirm`; o diálogo só fecha sozinho se a confirmação terminar sem erro.
 */
function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  destructive = false,
  busy = false,
  onConfirm,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  busy?: boolean;
  onConfirm: () => void | Promise<void>;
  /** Campos extras, como "digite seu login para confirmar". */
  children?: React.ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={title} description={description}>
        {children}
        <div className="mt-5 flex justify-end gap-2">
          <DialogClose render={<Button variant="outline" disabled={busy} />}>Cancelar</DialogClose>
          <Button variant={destructive ? "destructive" : "default"} disabled={busy} onClick={() => void onConfirm()}>
            {busy ? "Aguarde…" : confirmLabel}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export { ConfirmDialog, Dialog, DialogClose, DialogContent, DialogTrigger };
