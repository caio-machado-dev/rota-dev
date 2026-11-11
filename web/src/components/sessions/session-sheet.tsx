"use client";

import { Dialog, DialogContent } from "@/components/ui/dialog";

import { useSessionUi } from "./session-context";
import { SessionForm } from "./session-form";

/** Painel de registro de sessão: lateral no desktop, bottom sheet no celular. O formulário remonta a cada abertura. */
export function SessionSheet() {
  const { open, setOpen, prefill } = useSessionUi();
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent title="Registrar sessão" description="Estudo, construção, revisão ou certificação." placement="responsive">
        <SessionForm prefill={prefill} onDone={() => setOpen(false)} />
      </DialogContent>
    </Dialog>
  );
}
