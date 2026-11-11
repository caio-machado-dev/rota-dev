"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Cronômetro simples. Guarda o instante de início e recalcula a cada segundo, então continua
 * certo mesmo se a aba ficar em segundo plano (o intervalo pode atrasar, o relógio não).
 */
export function useStopwatch() {
  const [running, setRunning] = useState(false);
  const [elapsedMs, setElapsedMs] = useState(0);
  const startedAt = useRef(0);
  const carried = useRef(0);

  useEffect(() => {
    if (!running) return;
    const id = setInterval(() => setElapsedMs(carried.current + Date.now() - startedAt.current), 1000);
    return () => clearInterval(id);
  }, [running]);

  const start = useCallback(() => {
    startedAt.current = Date.now();
    setRunning(true);
  }, []);

  const pause = useCallback(() => {
    carried.current += Date.now() - startedAt.current;
    setElapsedMs(carried.current);
    setRunning(false);
  }, []);

  const reset = useCallback(() => {
    carried.current = 0;
    setElapsedMs(0);
    setRunning(false);
  }, []);

  return { running, elapsedSeconds: Math.floor(elapsedMs / 1000), start, pause, reset };
}

/** 3725 → "01:02:05". */
export function clock(totalSeconds: number): string {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [h, m, s].map((n) => String(n).padStart(2, "0")).join(":");
}
