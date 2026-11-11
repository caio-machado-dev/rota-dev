import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { ProgressOverview } from "./progress-overview";

afterEach(() => vi.unstubAllGlobals());

const point = (weekStart: string, score: number) => ({ weekStart, value: { score, skills: 30, gates: 20, study: 60, habit: 50 } });
const readiness = { current: { score: 42, skills: 30, gates: 20, study: 60, habit: 50 }, history: [point("2026-11-02", 35), point("2026-11-09", 42)] };
const now = { today: "2026-11-10", weekday: 2, templateToday: [], sessionsToday: [], minutesToday: 0, minutesThisWeek: 90, weeklyBudgetMinutes: 600 };

describe("ProgressOverview", () => {
  it("mostra o índice, a composição com pesos, a variação e a consistência", async () => {
    stubApi({
      "/api/readiness": readiness,
      "/api/now": now,
      "/api/diagnosis/latest": null,
      "/api/tracking/sessions?from=2026-08-19&to=2026-11-10": [{ id: "s1", day: "2026-11-10", minutes: 90, kind: "STUDY", topic: "x" }],
    });
    renderScreen(<ProgressOverview />);

    expect(await screen.findByRole("img", { name: /Prontidão 42 de 100, Em construção/ })).toBeInTheDocument();
    expect(screen.getByText("+7 pts desde o último ponto")).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "Composição do índice" })).toBeInTheDocument();
    expect(await screen.findByRole("img", { name: /Consistência: 1 dias com estudo/ })).toBeInTheDocument();
    expect(screen.getByText("Sem diagnóstico pronto")).toBeInTheDocument();
  });

  it("sem sessões, o mix de tipos mostra o estado vazio com a dica de registrar", async () => {
    stubApi({ "/api/readiness": readiness, "/api/now": now, "/api/diagnosis/latest": null, "/api/tracking/sessions?from=2026-08-19&to=2026-11-10": [] });
    renderScreen(<ProgressOverview />);

    expect(await screen.findByText("Você ainda não registrou sessões")).toBeInTheDocument();
  });

  it("mostra erro com tentar de novo quando o índice não carrega", async () => {
    stubApi({ "/api/readiness": null });
    renderScreen(<ProgressOverview />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Não consegui carregar o progresso");
  });
});
