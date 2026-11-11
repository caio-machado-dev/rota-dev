"use client";

import { useEffect } from "react";

const SEQUENCE_WINDOW_MS = 1000;

/** Não dispara atalho de uma tecla enquanto a pessoa digita num campo. */
export function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
}

/**
 * Atalhos globais:
 * - ⌘/Ctrl + K alterna a paleta de comandos (funciona até dentro de campos, como no navegador);
 * - "g" seguido de uma tecla em até 1 s é um atalho de navegação (g a, g p...), via `onSequence`;
 * - teclas soltas (n, c) chamam `onKey`.
 * Atalhos com Ctrl/⌘/Alt (do navegador) nunca são interceptados, exceto ⌘K.
 */
export function useGlobalShortcuts(handlers: {
  onTogglePalette: () => void;
  onSequence: (second: string) => void;
  onKey?: (key: string) => void;
}) {
  const { onTogglePalette, onSequence, onKey } = handlers;

  useEffect(() => {
    let pendingG = 0;

    function onKeyDown(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        onTogglePalette();
        return;
      }
      if (event.metaKey || event.ctrlKey || event.altKey || event.repeat) return;
      if (isTypingTarget(event.target)) return;

      const key = event.key.toLowerCase();
      if (pendingG && Date.now() - pendingG < SEQUENCE_WINDOW_MS) {
        pendingG = 0;
        onSequence(key);
        return;
      }
      if (key === "g") {
        pendingG = Date.now();
        return;
      }
      onKey?.(key);
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onTogglePalette, onSequence, onKey]);
}
