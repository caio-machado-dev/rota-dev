import { fireEvent, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { compatibility, DiagnosisSection } from "./diagnosis-section";

afterEach(() => vi.unstubAllGlobals());

const axes = [
  { key: "java", name: "Java", score: 3, required: 4, verified: true, rationale: "Três projetos em Java.", evidences: [{ id: "e1", title: "repo/pedidos", url: "https://github.com/x/pedidos" }] },
  { key: "testes", name: "Testes de integração", score: 1, required: 4, verified: false, evidences: [] },
];
const diagnosis = {
  id: "d1",
  status: "READY",
  createdAt: "2026-11-01T10:00:00Z",
  estimatedLevel: "JUNIOR",
  summary: "Base sólida em Python; falta Java em produção.",
  axes,
  gaps: [{ axisKey: "testes", axisName: "Testes de integração", description: "Sem testes com banco real.", priority: 1, size: 3 }],
};
const goal = { targetRole: "Backend Java", targetLevel: "PLENO", track: "BACKEND", deadline: "2027-06-30", hoursPerWeek: 10, wantsCertifications: false };

describe("DiagnosisSection", () => {
  it("calcula a compatibilidade limitando cada nota ao exigido", () => {
    expect(compatibility(axes)).toBe(50); // (3 + 1) / (4 + 4)
    expect(compatibility([{ ...axes[0], score: 5 }])).toBe(100);
  });

  it("mostra nível, compatibilidade e lacunas; a lacuna prioritária já vem selecionada", async () => {
    stubApi({ "/api/diagnosis/latest": diagnosis, "/api/goal": goal });
    renderScreen(<DiagnosisSection />);

    expect(await screen.findByText(/Nível estimado: Júnior/)).toBeInTheDocument();
    expect(screen.getByText("compatibilidade com o cargo")).toBeInTheDocument();
    const detail = screen.getByLabelText("Detalhe: Testes de integração");
    expect(within(detail).getByText("Não verificado")).toBeInTheDocument();
    expect(within(detail).getByText(/Nenhuma evidência encontrada/)).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "Lacunas por prioridade" })).toBeInTheDocument();
  });

  it("ao selecionar uma competência, mostra justificativa e evidência com link", async () => {
    stubApi({ "/api/diagnosis/latest": diagnosis, "/api/goal": goal });
    renderScreen(<DiagnosisSection />);

    fireEvent.click(await screen.findByRole("button", { name: /^Java/ }));
    const detail = screen.getByLabelText("Detalhe: Java");
    expect(within(detail).getByText(/Três projetos em Java/)).toBeInTheDocument();
    expect(within(detail).getByRole("link", { name: /repo\/pedidos/ })).toHaveAttribute("href", "https://github.com/x/pedidos");
  });

  it("sem diagnóstico, mostra o estado vazio e gera pelo botão", async () => {
    const calls = stubApi({ "/api/diagnosis/latest": null, "/api/goal": goal }, () => Response.json({ id: "d2" }, { status: 202 }));
    renderScreen(<DiagnosisSection />);

    expect(await screen.findByText("Ainda não há diagnóstico")).toBeInTheDocument();
    const button = screen.getByRole("button", { name: /Gerar diagnóstico/ });
    await vi.waitFor(() => expect(button).toBeEnabled());
    fireEvent.click(button);
    await vi.waitFor(() => expect(calls.some((c) => c.method === "POST" && c.path === "/api/diagnosis")).toBe(true));
  });

  it("mostra o job em andamento sem inventar etapas", async () => {
    stubApi({ "/api/diagnosis/latest": { ...diagnosis, status: "PENDING", axes: [], gaps: [] }, "/api/goal": goal });
    renderScreen(<DiagnosisSection />);

    expect(await screen.findByText(/Lendo suas fontes/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Gerando/ })).toBeDisabled();
  });
});
