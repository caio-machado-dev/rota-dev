import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PlanView } from "./plan-view";

afterEach(() => vi.unstubAllGlobals());

const v2 = {
  id: "p2",
  version: 2,
  status: "READY",
  reason: "Plano refeito a pedido",
  startsOn: "2026-11-10",
  createdAt: "2026-11-10T12:00:00Z",
  content: {
    summary: "Testes primeiro, depois APIs.",
    anchorProject: { title: "Rota Dev", description: "Evoluir o projeto", deliverables: ["CI verde"], evidenceIds: [] },
    quarters: [
      {
        index: 1,
        title: "Testes",
        focus: "Testes de integração",
        milestones: [{ title: "Testcontainers no CI", axisKey: "testing", criteria: ["PR mergeado com testes"] }],
        weeks: [{ index: 1, theme: "Base", totalMinutes: 90, tasks: [{ title: "Revisar diff", kind: "REVIEW", minutes: 90 }] }],
      },
    ],
    resources: [],
  },
};
const v1 = { ...v2, id: "p1", version: 1, reason: "Primeira versão" };

describe("PlanView", () => {
  it("mostra projeto-âncora, portões, semanas e o histórico com o motivo", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string) => Response.json(path === "/api/plan/versions" ? [v2, v1] : v2)),
    );

    render(<PlanView />);

    expect(await screen.findByText("Rota Dev")).toBeInTheDocument();
    expect(screen.getByText("Portão: Testcontainers no CI")).toBeInTheDocument();
    expect(screen.getByText("Revisão: Revisar diff")).toBeInTheDocument();
    expect(screen.getByText("Semana 1 · Base").nextSibling).toHaveTextContent("1h30");
    expect(screen.getByText(/Primeira versão/)).toBeInTheDocument();
  });

  it("explica o que falta quando a API recusa (409)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string, init?: RequestInit) => {
        if (init?.method === "POST") return Response.json({ detail: "Gere o diagnóstico antes do plano." }, { status: 409 });
        return path === "/api/plan/versions" ? Response.json([]) : new Response(null, { status: 404 });
      }),
    );

    render(<PlanView />);
    fireEvent.click(await screen.findByRole("button", { name: "Gerar plano" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Gere o diagnóstico antes do plano.");
  });
});
