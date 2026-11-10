import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";

import { ProgressView } from "./progress-view";

beforeAll(() => {
  vi.stubGlobal(
    "ResizeObserver",
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  );
});

afterEach(() => {
  // Mantém o ResizeObserver; tira só o fetch de cada teste.
  vi.stubGlobal("fetch", undefined);
});

const readiness = {
  current: { score: 60, skills: 50, gates: 50, study: 100, habit: 50 },
  history: [
    { weekStart: "2026-11-02", value: { score: 48, skills: 50, gates: 0, study: 100, habit: 50 } },
    { weekStart: "2026-11-09", value: { score: 60, skills: 50, gates: 50, study: 100, habit: 50 } },
  ],
};
const plan = {
  id: "p1",
  version: 1,
  status: "READY",
  reason: "Primeira versão",
  startsOn: "2026-11-09",
  createdAt: "2026-11-09T12:00:00Z",
  content: {
    summary: "",
    anchorProject: { title: "", description: "", deliverables: [], evidenceIds: [] },
    resources: [],
    quarters: [
      {
        index: 1,
        title: "T1",
        focus: "",
        weeks: [],
        milestones: [
          { title: "Testcontainers no CI", axisKey: "testing", criteria: ["PR"] },
          { title: "API publicada", axisKey: "apis", criteria: ["Deploy"] },
        ],
      },
    ],
  },
};
const achieved = [{ id: "a1", planVersion: 1, milestone: "Testcontainers no CI", evidenceUrl: "https://github.com/x/pull/1", achievedOn: "2026-11-12" }];

function mockApi(checkin: object, onPost?: (path: string, body: unknown) => Response) {
  const fetchMock = vi.fn(async (path: string, init?: RequestInit) => {
    if (init?.method === "POST" && onPost) return onPost(path, JSON.parse(String(init.body)));
    if (path === "/api/readiness") return Response.json(readiness);
    if (path === "/api/checkin") return Response.json(checkin);
    if (path === "/api/plan/current") return Response.json(plan);
    if (path === "/api/milestones") return Response.json(achieved);
    return new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("ProgressView", () => {
  it("mostra o índice, as partes com peso, os portões e o check-in fechado fora da janela", async () => {
    mockApi({ minutesLogged: 0, reviewDays: 0 });

    render(<ProgressView />);

    expect((await screen.findByText("de 100")).previousSibling).toHaveTextContent("60");
    expect(screen.getByText("· peso 40")).toBeInTheDocument();
    expect(screen.getByText("· 1 de 2")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "prova" })).toHaveAttribute("href", "https://github.com/x/pull/1");
    expect(screen.getByText(/abre no sábado/)).toBeInTheDocument();
    // Só o portão pendente aparece para registrar.
    expect(screen.getByRole("option", { name: "API publicada" })).toBeInTheDocument();
  });

  it("fecha a semana no check-in aberto", async () => {
    const fetchMock = mockApi({ openWeek: "2026-11-09", minutesLogged: 150, reviewDays: 2 }, () => new Response(null, { status: 201 }));

    render(<ProgressView />);
    expect(await screen.findByText(/2h30 registradas · 2 dias/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Commits na semana"), { target: { value: "12" } });
    fireEvent.submit(screen.getByLabelText("Commits na semana").closest("form")!);

    await waitFor(() => expect(fetchMock.mock.calls.some(([path, init]) => path === "/api/checkin" && init?.method === "POST")).toBe(true));
    const post = fetchMock.mock.calls.find(([path, init]) => path === "/api/checkin" && init?.method === "POST");
    expect(JSON.parse(String(post?.[1]?.body))).toMatchObject({ commits: 12, energy: 3 });
  });
});
