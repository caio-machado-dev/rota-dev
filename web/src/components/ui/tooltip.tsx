"use client";

import { Tooltip as TooltipPrimitive } from "@base-ui/react/tooltip";

/**
 * Dica curta ao passar o mouse ou focar com o teclado. Use só para rotular ícones; informação importante vai em texto visível.
 * Em telas sem hover (celular) ela simplesmente não aparece, então o nome acessível (`aria-label`) continua no elemento.
 */
export function Tooltip({ label, children, side = "right" }: { label: string; children: React.ReactElement; side?: "top" | "right" | "bottom" | "left" }) {
  return (
    <TooltipPrimitive.Root>
      <TooltipPrimitive.Trigger render={children} />
      <TooltipPrimitive.Portal>
        <TooltipPrimitive.Positioner side={side} sideOffset={8} className="z-[70]">
          <TooltipPrimitive.Popup className="rounded-control border border-border bg-popover px-2 py-1 text-micro text-popover-foreground shadow-raised transition-opacity duration-150 data-[starting-style]:opacity-0 data-[ending-style]:opacity-0">
            {label}
          </TooltipPrimitive.Popup>
        </TooltipPrimitive.Positioner>
      </TooltipPrimitive.Portal>
    </TooltipPrimitive.Root>
  );
}

export const TooltipProvider = TooltipPrimitive.Provider;
