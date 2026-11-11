import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { AdjustmentsSection } from "./adjustments-section";

afterEach(() => vi.unstubAllGlobals());

const proposed = { id: "a1", kind: "REDUCE", reason: "Você registrou 40% das horas previstas.", status: "PROPOSED", weekStart: "2026-11-16", createdAt: "2026-11-09T10:00:00Z" };

describe("AdjustmentsSection", () => {
  it("só aceita depois da confirmação explícita", async () => {
    const calls = stubApi({ "/api/adjustments": [proposed], "/api/plan/versions": [] });
    renderScreen(<AdjustmentsSection />);

    fireEvent.click(await screen.findByRole("button", { name: "Aceitar e refazer o plano" }));
    const dialog = await screen.findByRole("dialog", { name: "Aceitar este reajuste?" });
    expect(calls.filter((c) => c.method === "POST")).toHaveLength(0);

    fireEvent.click(within(dialog).getByRole("button", { name: "Aceitar reajuste" }));
    await waitFor(() => expect(calls.some((c) => c.path === "/api/adjustments/a1/accept")).toBe(true));
  });

  it("recusa direto, sem mexer no plano", async () => {
    const calls = stubApi({ "/api/adjustments": [proposed], "/api/plan/versions": [] });
    renderScreen(<AdjustmentsSection />);

    fireEvent.click(await screen.findByRole("button", { name: "Manter o plano" }));
    await waitFor(() => expect(calls.some((c) => c.path === "/api/adjustments/a1/reject")).toBe(true));
  });

  it("reajuste aceito mostra o antes e o depois entre as versões", async () => {
    const content = (task: string) => ({
      summary: "s",
      anchorProject: { title: "API", description: "", deliverables: [], evidenceIds: [] },
      resources: [],
      quarters: [{ index: 1, title: "T1", focus: "", milestones: [], weeks: [{ index: 1, theme: "Testes", totalMinutes: 60, tasks: [{ kind: "STUDY", title: task, minutes: 60 }] }] }],
    });
    const v1 = { id: "v1id", version: 1, status: "READY", reason: "inicial", createdAt: "2026-10-01T10:00:00Z", startsOn: "2026-10-05", content: content("Ler docs") };
    const v2 = { id: "v2id", version: 2, status: "READY", reason: "reajuste", createdAt: "2026-11-10T10:00:00Z", startsOn: "2026-10-05", content: content("Escrever teste") };
    stubApi({
      "/api/adjustments": [{ ...proposed, status: "ACCEPTED", planVersionId: "v2id" }],
      "/api/plan/versions": [v1, v2],
      "/api/plan/versions/v1id": v1,
      "/api/plan/versions/v2id": v2,
    });
    renderScreen(<AdjustmentsSection />);

    fireEvent.click(await screen.findByText(/Ver o que mudou \(v1 → v2\)/));
    expect(await screen.findByText(/Estudo: Escrever teste/)).toBeInTheDocument();
  });

  it("sem reajustes, mostra o estado vazio", async () => {
    stubApi({ "/api/adjustments": [] });
    renderScreen(<AdjustmentsSection />);
    expect(await screen.findByText("Nenhum reajuste por enquanto")).toBeInTheDocument();
  });
});
