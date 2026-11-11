import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { FoundSection } from "./found-section";

let query = "";
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(query) }));

afterEach(() => {
  vi.unstubAllGlobals();
  query = "";
});

/** A lista de habilidades (o card de comparação também cita nomes, por isso os testes olham só aqui). */
const skillsCard = () => within(screen.getByText("Habilidades identificadas").closest("section")!);

const skill = (name: string, weight: number, confidence: string) => ({ name, weight, confidence });
const profile = {
  snapshots: [
    {
      id: "g2",
      sourceType: "GITHUB",
      createdAt: "2026-11-10T12:00:00Z",
      skills: [skill("Java", 0.7, "HIGH"), skill("Docker", 0.4, "HIGH")],
      evidences: [{ id: "e1", kind: "REPO", title: "rota-dev", skill: "Java", url: "https://github.com/x/rota-dev", confidence: "HIGH" }],
    },
    { id: "g1", sourceType: "GITHUB", createdAt: "2026-10-01T12:00:00Z", skills: [skill("Java", 0.3, "HIGH"), skill("Python", 0.8, "HIGH")], evidences: [] },
    { id: "r1", sourceType: "RESUME", createdAt: "2026-11-01T12:00:00Z", skills: [skill("Scrum", 0.5, "LOW")], evidences: [] },
  ],
};

describe("FoundSection", () => {
  it("lista as habilidades das leituras mais recentes, com confiança e fonte", async () => {
    stubApi({ "/api/profile": profile });
    renderScreen(<FoundSection />);

    await screen.findByText("Habilidades identificadas");
    expect(skillsCard().getByText("Java")).toBeInTheDocument();
    expect(skillsCard().getByText("Scrum")).toBeInTheDocument();
    expect(skillsCard().queryByText("Python")).not.toBeInTheDocument(); // só existe na leitura antiga
    expect(skillsCard().getByRole("button", { name: /Scrum.*Declarado/ })).toBeInTheDocument();
  });

  it("filtra por fonte, confiança e nome", async () => {
    stubApi({ "/api/profile": profile });
    renderScreen(<FoundSection />);
    await screen.findByText("Habilidades identificadas");

    fireEvent.change(screen.getByLabelText("Filtrar por fonte"), { target: { value: "RESUME" } });
    expect(skillsCard().queryByText("Java")).not.toBeInTheDocument();
    expect(skillsCard().getByText("Scrum")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Filtrar por fonte"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("Filtrar por confiança"), { target: { value: "HIGH" } });
    expect(skillsCard().queryByText("Scrum")).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Buscar habilidade"), { target: { value: "dock" } });
    expect(skillsCard().queryByText("Java")).not.toBeInTheDocument();
    expect(skillsCard().getByText("Docker")).toBeInTheDocument();
  });

  it("abre a evidência de uma habilidade e vem filtrado quando a paleta manda ?q=", async () => {
    query = "q=java";
    stubApi({ "/api/profile": profile });
    renderScreen(<FoundSection />);

    expect(await screen.findByLabelText("Buscar habilidade")).toHaveValue("java");
    expect(skillsCard().queryByText("Docker")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Java/ }));
    expect(screen.getByRole("link", { name: /rota-dev/ })).toHaveAttribute("href", "https://github.com/x/rota-dev");
  });

  it("compara as duas leituras mais recentes de uma fonte", async () => {
    stubApi({ "/api/profile": profile });
    renderScreen(<FoundSection />);

    const compare = (await screen.findByText("Comparar leituras")).closest("section")!;
    expect(within(compare).getByText(/Docker/)).toBeInTheDocument(); // entrou
    expect(within(compare).getByText(/Python/)).toBeInTheDocument(); // saiu
    expect(within(compare).getByText(/30% → 70%/)).toBeInTheDocument(); // Java mudou
  });

  it("sem leituras, mostra o estado vazio", async () => {
    stubApi({ "/api/profile": { snapshots: [] } });
    renderScreen(<FoundSection />);
    expect(await screen.findByText("Ainda não encontramos nada")).toBeInTheDocument();
  });
});
