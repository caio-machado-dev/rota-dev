import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdjustmentsList } from "./adjustments-list";

afterEach(() => vi.unstubAllGlobals());

const proposed = {
  id: "a1",
  weekStart: "2026-11-09",
  kind: "LIGHTER_WEEK",
  reason: "Semana pesada: energia 2/5 e um bloqueio (mudança).",
  status: "PROPOSED",
  createdAt: "2026-11-15T15:00:00Z",
};

describe("AdjustmentsList", () => {
  it("aceita a proposta e mostra a decisão", async () => {
    let decided = false;
    const fetchMock = vi.fn(async (_path: string, init?: RequestInit) => {
      if (init?.method === "POST") {
        decided = true;
        return Response.json({ planVersionId: "p2" }, { status: 202 });
      }
      return Response.json([decided ? { ...proposed, status: "ACCEPTED" } : proposed]);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AdjustmentsList />);
    expect(await screen.findByText(/Semana pesada/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Aceitar e refazer o plano" }));

    expect(await screen.findByText("aceito: nova versão do plano")).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/adjustments/a1/accept", expect.anything()));
  });

  it("sem reajustes, não mostra nada", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json([])));

    const { container } = render(<AdjustmentsList />);

    await waitFor(() => expect(container).toBeEmptyDOMElement());
  });
});
