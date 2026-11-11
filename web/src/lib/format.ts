/** Formatação e regras de apresentação puras (sem React, sem rede), fáceis de testar. */

export const KIND_LABEL: Record<string, string> = {
  STUDY: "Estudo",
  BUILD: "Construção",
  REVIEW: "Revisão",
  CERT: "Certificação",
};

export const SESSION_KINDS = ["STUDY", "BUILD", "REVIEW", "CERT"] as const;

/** 150 → "2h30"; 45 → "45 min"; 120 → "2h". */
export function duration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, "0")}`;
}

/** "Boa noite" etc. na hora do fuso do usuário. */
export function greeting(now: Date, timeZone: string): string {
  const hour = Number(new Intl.DateTimeFormat("en-GB", { hour: "numeric", hourCycle: "h23", timeZone }).format(now));
  if (hour < 5) return "Boa noite";
  if (hour < 12) return "Bom dia";
  if (hour < 18) return "Boa tarde";
  return "Boa noite";
}

/** Primeiro nome para a saudação; cai no login se não houver nome. */
export function firstName(me: { name?: string | null; login: string }): string {
  return (me.name?.trim().split(/\s+/)[0] || me.login) as string;
}

/** "10 de novembro" a partir de `YYYY-MM-DD`, sem deslocar o dia por fuso. */
export function longDate(isoDay: string): string {
  return new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "long" }).format(new Date(`${isoDay}T12:00:00`));
}

export type Pace = "ahead" | "on-track" | "behind" | "none";

/**
 * Ritmo da semana: compara o que foi estudado com a fração da meta esperada até hoje
 * (`weekday` 1 = segunda ... 7 = domingo). Tolerância de 15% da meta para "no ritmo".
 */
export function pace(minutesThisWeek: number, budget: number | null | undefined, weekday: number): Pace {
  if (!budget) return "none";
  const expected = (budget * weekday) / 7;
  if (minutesThisWeek >= budget || minutesThisWeek > expected + budget * 0.15) return "ahead";
  if (minutesThisWeek >= expected - budget * 0.15) return "on-track";
  return "behind";
}

export const PACE_LABEL: Record<Exclude<Pace, "none">, string> = {
  ahead: "Adiantado",
  "on-track": "No ritmo",
  behind: "Abaixo do ritmo",
};

/** Variação entre os dois últimos pontos semanais do histórico; `null` se há menos de dois. */
export function readinessDelta(history: { weekStart: string; value: { score: number } }[]): number | null {
  if (history.length < 2) return null;
  const sorted = [...history].sort((x, y) => x.weekStart.localeCompare(y.weekStart));
  return sorted[sorted.length - 1].value.score - sorted[sorted.length - 2].value.score;
}

/** Faixa de prontidão em palavras. */
export function readinessBand(score: number): string {
  if (score >= 85) return "Pronto";
  if (score >= 65) return "Avançado";
  if (score >= 40) return "Em construção";
  return "Início";
}
