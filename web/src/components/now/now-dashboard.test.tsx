import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { SWRConfig } from "swr";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SessionProvider } from "@/components/sessions/session-context";
import { SessionSheet } from "@/components/sessions/session-sheet";
import { ToastProvider } from "@/components/ui/toast";

import { NowDashboard } from "./now-dashboard";

afterEach(() => vi.unstubAllGlobals());

const me = { id: "0193f0c2-0000-7000-8000-000000000001", login: "caio", name: "Caio Machado", timeZone: "America/Sao_Paulo", tier: "FREE" };
const baseNow = {
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
const readiness = {
  current: { score: 42, skills: 30, gates: 20, study: 60, habit: 50 },
  history: [
    { weekStart: "2026-11-02", value: { score: 35, skills: 0, gates: 0, study: 0, habit: 0 } },
    { weekStart: "2026-11-09", value: { score: 42, skills: 0, gates: 0, study: 0, habit: 0 } },
  ],
};

type Overrides = Record<string, unknown>;

function stubApi(overrides: Overrides = {}, posts: { path: string; body: unknown }[] = []) {
  const data: Overrides = {
    "/api/me": me,
    "/api/now": baseNow,
    "/api/readiness": readiness,
    "/api/checkin": { minutesLogged: 150, reviewDays: 1, openWeek: "2026-11-09" },
    "/api/milestones": [],
    "/api/adjustments": [],
    ...overrides,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      if (init?.method === "POST" || init?.method === "PUT") {
        posts.push({ path, body: JSON.parse(String(init.body)) });
        return Response.json({ id: "s2", ...baseNow.sessionsToday[0] }, { status: 201 });
      }
      return path in data && data[path] !== null ? Response.json(data[path]) : new Response(null, { status: 404 });
    }),
  );
}

function renderDashboard() {
  return render(
    <SWRConfig value={{ provider: () => new Map() }}>
      <ToastProvider>
        <SessionProvider>
          <NowDashboard />
          <SessionSheet />
        </SessionProvider>
      </ToastProvider>
    </SWRConfig>,
  );
}

describe("NowDashboard", () => {
  it("mostra saudação, semana do plano, ritmo, prontidão com composição e sessões de hoje", async () => {
    stubApi();
    renderDashboard();

    expect(await screen.findByRole("heading", { level: 1, name: /, Caio\.$/ })).toBeInTheDocument();
    expect(screen.getByText("Semana 2 · Testes")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "Horas da semana" })).toHaveAttribute("aria-valuenow", "150");
    expect(screen.getByText("Transações")).toBeInTheDocument();
    const composition = await screen.findByRole("list", { name: "Composição do índice" });
    expect(within(composition).getByText(/Habilidades/)).toBeInTheDocument();
    expect(screen.getByText("+7 pts")).toBeInTheDocument();
    expect(screen.getByLabelText("1 de 3 passos")).toBeInTheDocument();
    expect(screen.getByLabelText("Li o diff inteiro")).toBeChecked();
  });

  it("registra uma sessão a partir de uma tarefa do plano, com o formulário já preenchido", async () => {
    const posts: { path: string; body: unknown }[] = [];
    stubApi({}, posts);
    renderDashboard();

    fireEvent.click(await screen.findByRole("button", { name: "Registrar sessão de Revisar diff" }));
    const topic = await screen.findByLabelText("Tema");
    expect(topic).toHaveValue("Revisar diff");

    fireEvent.click(screen.getByRole("button", { name: "Registrar sessão", hidden: false }));
    await waitFor(() => expect(posts.some((p) => p.path === "/api/tracking/sessions")).toBe(true));
    expect(posts.find((p) => p.path === "/api/tracking/sessions")?.body).toMatchObject({ topic: "Revisar diff", kind: "REVIEW", minutes: 90 });
  });

  it("marcar um passo do protocolo salva o dia", async () => {
    const posts: { path: string; body: unknown }[] = [];
    stubApi({}, posts);
    renderDashboard();

    fireEvent.click(await screen.findByLabelText("Consigo explicar o que o código faz"));
    await waitFor(() => expect(posts.some((p) => p.path === "/api/tracking/review-habit")).toBe(true));
    expect(posts.find((p) => p.path === "/api/tracking/review-habit")?.body).toMatchObject({ day: "2026-11-10", readDiff: true, explained: true, tested: false });
  });

  it("sem plano, o estado vazio aponta para a página do plano", async () => {
    stubApi({ "/api/now": { ...baseNow, planWeek: null, templateToday: [] } });
    renderDashboard();

    expect(await screen.findByRole("link", { name: "Montar meu plano" })).toHaveAttribute("href", "/plano");
  });

  it("mostra erro com tentar de novo quando /api/now falha", async () => {
    stubApi({ "/api/now": null });
    renderDashboard();

    expect(await screen.findByRole("alert")).toHaveTextContent("Não consegui carregar o seu dia");
  });
});
