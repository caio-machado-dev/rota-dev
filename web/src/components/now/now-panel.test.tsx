import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { NowPanel } from "./now-panel";

afterEach(() => vi.unstubAllGlobals());

const now = {
  today: "2026-11-10",
  weekday: 2,
  planWeek: { planVersion: 1, index: 2, theme: "Testes", totalMinutes: 90, tasks: [{ title: "Revisar diff", kind: "REVIEW", minutes: 90 }] },
  templateToday: [{ weekday: 2, kind: "STUDY", minutes: 60 }],
  sessionsToday: [{ id: "s1", day: "2026-11-10", kind: "STUDY", minutes: 45, topic: "Transações", learning: "readOnly pula o dirty checking" }],
  minutesToday: 45,
  minutesThisWeek: 150,
  weeklyBudgetMinutes: 600,
  habitToday: { day: "2026-11-10", readDiff: true, explained: false, tested: false, stepsDone: 1, complete: false },
};

describe("NowPanel", () => {
  it("mostra a semana do plano, o progresso da semana e as sessões de hoje", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json(now)));

    render(<NowPanel />);

    expect(await screen.findByText("Semana 2 do plano · Testes")).toBeInTheDocument();
    expect(screen.getByText("2h30")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "Horas da semana" })).toHaveAttribute("aria-valuenow", "150");
    expect(screen.getByText("Transações")).toBeInTheDocument();
    expect(screen.getByText(/1 de 4/)).toBeInTheDocument();
    expect(screen.getByLabelText("Li o diff inteiro")).toBeChecked();
  });

  it("registra uma sessão e recarrega o dia", async () => {
    const fetchMock = vi.fn(async (_path: string, init?: RequestInit) =>
      init?.method === "POST" ? Response.json({ ...now.sessionsToday[0], id: "s2" }, { status: 201 }) : Response.json(now),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<NowPanel />);
    fireEvent.change(await screen.findByLabelText("Tema"), { target: { value: "Testcontainers" } });
    fireEvent.submit(screen.getByLabelText("Tema").closest("form")!);

    await waitFor(() => expect(fetchMock.mock.calls.filter(([path]) => path === "/api/now")).toHaveLength(2));
    const post = fetchMock.mock.calls.find(([, init]) => init?.method === "POST");
    expect(JSON.parse(String(post?.[1]?.body))).toMatchObject({ topic: "Testcontainers", kind: "STUDY", minutes: 30 });
  });

  it("sem plano, aponta para a página do plano", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({ ...now, planWeek: undefined })));

    render(<NowPanel />);

    expect(await screen.findByRole("link", { name: "Plano" })).toHaveAttribute("href", "/plano");
  });
});
