import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { CheckinSection } from "./checkin-section";

afterEach(() => vi.unstubAllGlobals());

const history = [{ weekStart: "2026-11-02", minutesLogged: 300, reviewDays: 3, commits: 12, energy: 4, blockers: "Testcontainers no CI" }];

describe("CheckinSection", () => {
  it("fechado: explica a janela e mostra o histórico", async () => {
    stubApi({ "/api/checkin": { minutesLogged: 120, reviewDays: 1 }, "/api/checkin/history": history, "/api/adjustments": [] });
    renderScreen(<CheckinSection />);

    expect(await screen.findByText("Check-in fechado")).toBeInTheDocument();
    expect(screen.getByText(/abre no sábado e fica aberto até terça/)).toBeInTheDocument();
    expect(await screen.findByText(/Testcontainers no CI/)).toBeInTheDocument();
  });

  it("aberto: mostra o resumo automático e fecha a semana enviando os campos", async () => {
    const calls = stubApi(
      { "/api/checkin": { minutesLogged: 150, reviewDays: 2, openWeek: "2026-11-09" }, "/api/checkin/history": [], "/api/adjustments": [] },
      () => Response.json({}),
    );
    renderScreen(<CheckinSection />);

    expect(await screen.findByText("2h30")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/Commits na semana/), { target: { value: "9" } });
    fireEvent.click(screen.getByRole("radio", { name: "4" }));
    fireEvent.change(screen.getByLabelText("O que travou"), { target: { value: "CI lento" } });
    fireEvent.click(screen.getByRole("button", { name: "Fechar a semana" }));

    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.path === "/api/checkin")).toBe(true));
    expect(calls.find((c) => c.path === "/api/checkin")?.body).toMatchObject({ commits: 9, energy: 4, blockers: "CI lento" });
  });

  it("aberto com erro da API: mantém o que foi digitado e mostra a mensagem", async () => {
    stubApi(
      { "/api/checkin": { minutesLogged: 0, reviewDays: 0, openWeek: "2026-11-09" }, "/api/checkin/history": [], "/api/adjustments": [] },
      () => Response.json({ detail: "Energia fora do intervalo." }, { status: 400 }),
    );
    renderScreen(<CheckinSection />);

    fireEvent.change(await screen.findByLabelText("O que travou"), { target: { value: "Sem tempo" } });
    fireEvent.click(screen.getByRole("button", { name: "Fechar a semana" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Energia fora do intervalo.");
    expect(screen.getByLabelText("O que travou")).toHaveValue("Sem tempo");
  });

  it("concluído: mostra o resultado e o link para os reajustes sugeridos", async () => {
    stubApi({
      "/api/checkin": { minutesLogged: 300, reviewDays: 3, done: { weekStart: "2026-11-02", minutesLogged: 300, reviewDays: 3, commits: 12, energy: 4 } },
      "/api/checkin/history": history,
      "/api/adjustments": [{ id: "a1", kind: "REDUCE", reason: "Poucas horas", status: "PROPOSED", weekStart: "2026-11-16", createdAt: "2026-11-09T10:00:00Z" }],
    });
    renderScreen(<CheckinSection />);

    expect(await screen.findByText("Concluído", { selector: "span" })).toBeInTheDocument();
    expect(await screen.findByRole("link", { name: "Revisar e decidir" })).toHaveAttribute("href", "/progresso/reajustes");
  });
});
