import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { RoadmapSection } from "./roadmap-section";

afterEach(() => vi.unstubAllGlobals());

const plan = {
  id: "p1",
  version: 2,
  status: "READY",
  reason: "Plano inicial",
  createdAt: "2026-10-01T10:00:00Z",
  startsOn: "2026-10-05",
  content: {
    summary: "Foco em testes e transações.",
    anchorProject: { title: "API de pedidos", description: "API Spring com Postgres.", deliverables: ["CI verde"], evidenceIds: ["ev1"] },
    resources: [{ axisKey: "testes", kind: "BOOK", title: "Growing Object-Oriented Software", reason: "Base de TDD" }],
    quarters: [
      {
        index: 1,
        title: "Fundamentos",
        focus: "Spring e testes",
        milestones: [{ axisKey: "testes", title: "API testada", criteria: ["Cobertura de integração"] }],
        weeks: [
          { index: 1, theme: "Setup", totalMinutes: 60, tasks: [{ kind: "STUDY", title: "Ler docs", minutes: 60 }] },
          { index: 2, theme: "Testcontainers", totalMinutes: 120, tasks: [{ kind: "BUILD", title: "Primeiro teste", minutes: 120 }] },
        ],
      },
    ],
  },
};
const base = {
  "/api/plan/latest": plan,
  "/api/goal": { targetRole: "Backend", targetLevel: "PLENO", track: "BACKEND", deadline: "2027-06-30", hoursPerWeek: 10, wantsCertifications: false },
  "/api/diagnosis/latest": { id: "d1", status: "READY", createdAt: "2026-10-01T10:00:00Z", axes: [], gaps: [] },
  "/api/now": { today: "2026-11-10", weekday: 2, planWeek: { planVersion: 2, index: 2, theme: "Testcontainers", totalMinutes: 120, tasks: [] }, templateToday: [], sessionsToday: [], minutesToday: 0, minutesThisWeek: 0 },
  "/api/milestones": [{ id: "m1", milestone: "API testada", achievedOn: "2026-11-01", evidenceUrl: "https://x", planVersion: 2 }],
  "/api/profile": { snapshots: [{ id: "s1", sourceType: "GITHUB", createdAt: "2026-10-01T10:00:00Z", skills: [], evidences: [{ id: "ev1", kind: "REPO", title: "pedidos-api", url: "https://github.com/x/pedidos-api", confidence: "HIGH" }] }] },
};

describe("RoadmapSection", () => {
  it("mostra o projeto-âncora com a evidência resolvida e o portão conquistado na visão trimestral", async () => {
    stubApi(base);
    renderScreen(<RoadmapSection />);

    expect(await screen.findByRole("heading", { name: "API de pedidos" })).toBeInTheDocument();
    expect(await screen.findByRole("link", { name: /pedidos-api/ })).toHaveAttribute("href", "https://github.com/x/pedidos-api");
    expect(screen.getByText(/Conquistado em/)).toBeInTheDocument();
    expect(screen.getByText("Por que: Base de TDD")).toBeInTheDocument();
  });

  it("na visão semanal, destaca a semana atual e marca a anterior como concluída", async () => {
    stubApi(base);
    renderScreen(<RoadmapSection />);

    fireEvent.click(await screen.findByRole("tab", { name: "Semanas" }));
    const current = (await screen.findByText(/Semana 2 · Testcontainers/)).closest("li")!;
    expect(current).toHaveAttribute("aria-current", "step");
    expect(within(current).getByText("Semana atual")).toBeInTheDocument();
    expect(screen.getByText("Concluída")).toBeInTheDocument();
  });

  it("sem plano, explica o que falta e desabilita a geração sem diagnóstico", async () => {
    stubApi({ ...base, "/api/plan/latest": null, "/api/diagnosis/latest": null });
    renderScreen(<RoadmapSection />);

    expect(await screen.findByText("Ainda não há um plano")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Gerar plano/ })).toBeDisabled();
  });
});
