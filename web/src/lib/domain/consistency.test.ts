import { describe, expect, it } from "vitest";

import { addDays, heatmap, level, minutesByKind, weekStart, weeklyMinutes } from "./consistency";

const s = (day: string, minutes: number, kind = "STUDY") => ({ id: day + minutes, day, minutes, kind, topic: "x" });

describe("consistency", () => {
  it("acha a segunda-feira da semana e soma dias", () => {
    expect(weekStart("2026-11-10")).toBe("2026-11-09"); // terça
    expect(weekStart("2026-11-15")).toBe("2026-11-09"); // domingo
    expect(addDays("2026-11-30", 1)).toBe("2026-12-01");
  });

  it("classifica a intensidade do dia", () => {
    expect([0, 10, 45, 90, 180].map(level)).toEqual([0, 1, 2, 3, 4]);
  });

  it("monta o heatmap por semana, soma sessões do mesmo dia e deixa o futuro vazio", () => {
    const grid = heatmap([s("2026-11-10", 30), s("2026-11-10", 45), s("2026-11-02", 15)], "2026-11-10", 2);
    expect(grid).toHaveLength(2);
    expect(grid[0][0]).toMatchObject({ day: "2026-11-02", minutes: 15, level: 1 });
    expect(grid[1][1]).toMatchObject({ day: "2026-11-10", minutes: 75, level: 3 });
    expect(grid[1][2]).toBeNull(); // quarta, ainda não chegou
  });

  it("soma minutos por semana, incluindo semanas vazias", () => {
    const weeks = weeklyMinutes([s("2026-11-10", 60), s("2026-10-27", 30)], "2026-11-10", 3);
    expect(weeks).toEqual([
      { weekStart: "2026-10-26", minutes: 30 },
      { weekStart: "2026-11-02", minutes: 0 },
      { weekStart: "2026-11-09", minutes: 60 },
    ]);
  });

  it("agrupa minutos por tipo, do maior para o menor", () => {
    expect(minutesByKind([s("2026-11-10", 30, "BUILD"), s("2026-11-10", 90, "STUDY"), s("2026-11-09", 30, "BUILD")])).toEqual([
      { kind: "STUDY", minutes: 90 },
      { kind: "BUILD", minutes: 60 },
    ]);
  });
});
