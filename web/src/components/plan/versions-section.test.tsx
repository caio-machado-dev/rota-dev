import { fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { VersionsSection } from "./versions-section";

afterEach(() => vi.unstubAllGlobals());

const content = (taskTitle: string) => ({
  summary: "s",
  anchorProject: { title: "API", description: "", deliverables: [], evidenceIds: [] },
  resources: [],
  quarters: [{ index: 1, title: "T1", focus: "", milestones: [], weeks: [{ index: 1, theme: "Testes", totalMinutes: 60, tasks: [{ kind: "STUDY", title: taskTitle, minutes: 60 }] }] }],
});
const v1 = { id: "aaaaaaaa-0000-0000-0000-000000000001", version: 1, status: "READY", reason: "Plano inicial", createdAt: "2026-10-01T10:00:00Z", startsOn: "2026-10-05", content: content("Ler docs") };
const v2 = { id: "bbbbbbbb-0000-0000-0000-000000000002", version: 2, status: "READY", reason: "Reajuste aceito: menos horas", createdAt: "2026-10-20T10:00:00Z", startsOn: "2026-10-05", content: content("Escrever teste") };

describe("VersionsSection", () => {
  it("lista as versões como um git log, da mais nova para a mais antiga, com o motivo", async () => {
    stubApi({ "/api/plan/versions": [v1, v2] });
    renderScreen(<VersionsSection />);

    expect(await screen.findByText("Reajuste aceito: menos horas")).toBeInTheDocument();
    const labels = screen.getAllByText(/^v\d$/).map((e) => e.textContent);
    expect(labels).toEqual(["v2", "v1"]);
    expect(screen.getByText("atual")).toBeInTheDocument();
  });

  it("compara duas versões marcadas e mostra o que entrou e saiu", async () => {
    stubApi({
      "/api/plan/versions": [v1, v2],
      [`/api/plan/versions/${v1.id}`]: v1,
      [`/api/plan/versions/${v2.id}`]: v2,
    });
    renderScreen(<VersionsSection />);

    fireEvent.click(await screen.findByRole("checkbox", { name: /Comparar v1/ }));
    fireEvent.click(screen.getByRole("checkbox", { name: /Comparar v2/ }));

    expect(await screen.findByText(/Semana 1 · Estudo: Ler docs/)).toBeInTheDocument();
    expect(screen.getByText(/Semana 1 · Estudo: Escrever teste/)).toBeInTheDocument();
    expect(screen.getByLabelText("removido")).toBeInTheDocument();
  });

  it("sem versões, mostra o estado vazio", async () => {
    stubApi({ "/api/plan/versions": [] });
    renderScreen(<VersionsSection />);
    expect(await screen.findByText("Nenhuma versão ainda")).toBeInTheDocument();
  });
});
