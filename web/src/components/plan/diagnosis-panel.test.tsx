import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";

import { DiagnosisPanel } from "./diagnosis-panel";

// O ResponsiveContainer do Recharts mede o elemento com ResizeObserver, que o jsdom não tem.
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

afterEach(() => vi.unstubAllGlobals());

const ready = {
  id: "d1",
  status: "READY",
  createdAt: "2026-11-10T12:00:00Z",
  summary: "Base boa em Java, falta teste.",
  estimatedLevel: "JUNIOR",
  axes: [
    {
      key: "language",
      name: "Linguagem principal",
      required: 3,
      score: 3,
      verified: true,
      rationale: "Java no rota-dev",
      evidences: [{ id: "e1", title: "rota-dev", url: "https://github.com/x/rota-dev" }],
    },
    { key: "testing", name: "Testes", required: 3, score: 1, verified: false, rationale: "Sem evidência", evidences: [] },
  ],
  gaps: [{ axisKey: "testing", axisName: "Testes", size: 2, priority: 1, description: "Escrever testes de integração" }],
};

describe("DiagnosisPanel", () => {
  it("mostra nível, eixos com evidência, não verificado e lacunas", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json(ready)));

    render(<DiagnosisPanel />);

    expect(await screen.findByText("Nível estimado: Júnior")).toBeInTheDocument();
    expect(screen.getByText("1 de 3 · não verificado")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /rota-dev/ })).toHaveAttribute("href", "https://github.com/x/rota-dev");
    expect(screen.getByText(/faltam 2 pontos/)).toHaveTextContent("Escrever testes de integração");
  });

  it("explica o que falta quando a API recusa o pedido (409)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (_path: string, init?: RequestInit) =>
        init?.method === "POST"
          ? Response.json({ detail: "Defina o seu objetivo de carreira antes do diagnóstico." }, { status: 409 })
          : new Response(null, { status: 404 }),
      ),
    );

    render(<DiagnosisPanel />);
    fireEvent.click(await screen.findByRole("button", { name: "Gerar diagnóstico" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Defina o seu objetivo");
  });

  it("enquanto gera, desabilita o botão", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({ ...ready, status: "PENDING", axes: [], gaps: [] })));

    render(<DiagnosisPanel />);

    expect(await screen.findByRole("button", { name: "Gerando diagnóstico…" })).toBeDisabled();
  });
});
