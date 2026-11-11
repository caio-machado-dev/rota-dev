import type { StudySession } from "@/lib/api/client";

/** Cálculos de consistência a partir das sessões. Datas são strings `YYYY-MM-DD`, sem fuso (a API já devolve o dia do usuário). */

const DAY_MS = 86_400_000;
const toTime = (day: string) => Date.parse(`${day}T00:00:00Z`);
const toDay = (time: number) => new Date(time).toISOString().slice(0, 10);

export function addDays(day: string, amount: number): string {
  return toDay(toTime(day) + amount * DAY_MS);
}

/** Segunda-feira da semana de `day`. */
export function weekStart(day: string): string {
  const weekday = (new Date(toTime(day)).getUTCDay() + 6) % 7; // 0 = segunda
  return addDays(day, -weekday);
}

export type HeatCell = { day: string; minutes: number; level: 0 | 1 | 2 | 3 | 4 };

/** Nível de intensidade pelos minutos do dia: 0, até 29, até 59, até 119, 120 ou mais. */
export function level(minutes: number): HeatCell["level"] {
  if (minutes <= 0) return 0;
  if (minutes < 30) return 1;
  if (minutes < 60) return 2;
  if (minutes < 120) return 3;
  return 4;
}

/** Colunas = semanas (da mais antiga à atual), linhas = segunda a domingo. Dias futuros vêm como `null`. */
export function heatmap(sessions: StudySession[], today: string, weeks: number): (HeatCell | null)[][] {
  const byDay = new Map<string, number>();
  for (const s of sessions) byDay.set(s.day, (byDay.get(s.day) ?? 0) + s.minutes);
  const firstWeek = addDays(weekStart(today), -(weeks - 1) * 7);
  return Array.from({ length: weeks }, (_, w) =>
    Array.from({ length: 7 }, (_, d) => {
      const day = addDays(firstWeek, w * 7 + d);
      if (day > today) return null;
      const minutes = byDay.get(day) ?? 0;
      return { day, minutes, level: level(minutes) };
    }),
  );
}

/** Minutos por semana (segunda a domingo), da mais antiga à atual, incluindo semanas sem sessão. */
export function weeklyMinutes(sessions: StudySession[], today: string, weeks: number): { weekStart: string; minutes: number }[] {
  const firstWeek = addDays(weekStart(today), -(weeks - 1) * 7);
  const totals = new Map<string, number>();
  for (const s of sessions) {
    const key = weekStart(s.day);
    totals.set(key, (totals.get(key) ?? 0) + s.minutes);
  }
  return Array.from({ length: weeks }, (_, i) => {
    const key = addDays(firstWeek, i * 7);
    return { weekStart: key, minutes: totals.get(key) ?? 0 };
  });
}

/** Minutos por tipo de sessão, do maior para o menor. */
export function minutesByKind(sessions: StudySession[]): { kind: string; minutes: number }[] {
  const totals = new Map<string, number>();
  for (const s of sessions) totals.set(s.kind, (totals.get(s.kind) ?? 0) + s.minutes);
  return [...totals].map(([kind, minutes]) => ({ kind, minutes })).sort((a, b) => b.minutes - a.minutes);
}
