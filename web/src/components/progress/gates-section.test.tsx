import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { GatesSection } from "./gates-section";

afterEach(() => vi.unstubAllGlobals());

const plan = {
  id: "p1",
  version: 2,
  status: "READY",
  reason: "x",
  createdAt: "2026-10-01T10:00:00Z",
  startsOn: "2026-10-05",
  content: {
    summary: "s",
    anchorProject: { title: "API", description: "", deliverables: [], evidenceIds: [] },
    resources: [],
    quarters: [
      { index: 1, title: "Fundamentos", focus: "", weeks: [], milestones: [{ axisKey: "a", title: "API testada", criteria: ["Cobertura de integração"] }] },
      { index: 2, title: "Produção", focus: "", weeks: [], milestones: [{ axisKey: "b", title: "Deploy observável", criteria: ["Métricas"] }] },
    ],
  },
};

describe("GatesSection", () => {
  it("mostra conquistados com evidência, o próximo marco e os pendentes", async () => {
    stubApi({
      "/api/plan/current": plan,
      "/api/milestones": [{ id: "m1", milestone: "API testada", achievedOn: "2026-11-01", evidenceUrl: "https://github.com/x/api", planVersion: 2 }],
    });
    renderScreen(<GatesSection />);

    expect(await screen.findByText("1 de 2 portões conquistados")).toBeInTheDocument();
    expect(screen.getByText("Conquistado")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /evidência/ })).toHaveAttribute("href", "https://github.com/x/api");
    expect(screen.getByText("Próximo marco")).toBeInTheDocument();
  });

  it("registra a conquista com a URL de evidência", async () => {
    const calls = stubApi({ "/api/plan/current": plan, "/api/milestones": [] });
    renderScreen(<GatesSection />);

    fireEvent.click((await screen.findAllByRole("button", { name: "Registrar conquista" }))[0]);
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/URL da evidência/), { target: { value: "https://github.com/x/api" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Registrar conquista" }));

    await waitFor(() => expect(calls.some((c) => c.path === "/api/milestones" && c.method === "POST")).toBe(true));
    expect(calls.find((c) => c.path === "/api/milestones")?.body).toEqual({ milestone: "API testada", evidenceUrl: "https://github.com/x/api" });
  });

  it("sem plano, explica que os portões vêm com o plano", async () => {
    stubApi({ "/api/plan/current": null, "/api/milestones": [] });
    renderScreen(<GatesSection />);
    expect(await screen.findByText("Os portões aparecem com o plano")).toBeInTheDocument();
  });
});
