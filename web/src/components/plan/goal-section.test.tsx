import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { GoalSection } from "./goal-section";

afterEach(() => vi.unstubAllGlobals());

const goal = { targetRole: "Backend Java", targetLevel: "PLENO", track: "BACKEND", deadline: "2027-06-30", hoursPerWeek: 10, wantsCertifications: false };

describe("GoalSection", () => {
  it("sem objetivo, mostra o formulário e envia em PUT; depois vira o resumo", async () => {
    const calls = stubApi({ "/api/goal": null }, (call) => Response.json(call.body));
    renderScreen(<GoalSection />);

    fireEvent.change(await screen.findByLabelText("Cargo-alvo"), { target: { value: "Backend Java" } });
    fireEvent.change(screen.getByLabelText("Prazo"), { target: { value: "2027-06-30" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar objetivo" }));

    expect(await screen.findByRole("button", { name: "Editar" })).toBeInTheDocument();
    const put = calls.find((c) => c.method === "PUT");
    expect(put?.body).toMatchObject({ targetRole: "Backend Java", hoursPerWeek: 8, track: "BACKEND" });
    expect(screen.getByText("10h por semana".replace("10", "8"))).toBeInTheDocument();
  });

  it("com objetivo salvo, mostra o resumo e permite editar com os campos preenchidos", async () => {
    stubApi({ "/api/goal": goal });
    renderScreen(<GoalSection />);

    expect(await screen.findByText("Backend Java")).toBeInTheDocument();
    expect(screen.getByText("10h por semana")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Editar" }));
    expect(await screen.findByLabelText(/Horas por semana/)).toHaveValue(10);
  });

  it("mostra a mensagem da API quando o prazo é recusado e mantém o que foi digitado", async () => {
    stubApi({ "/api/goal": null }, () => Response.json({ detail: "O prazo precisa ser uma data futura." }, { status: 400 }));
    renderScreen(<GoalSection />);

    fireEvent.change(await screen.findByLabelText("Cargo-alvo"), { target: { value: "Backend" } });
    fireEvent.change(screen.getByLabelText("Prazo"), { target: { value: "2020-01-01" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar objetivo" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("O prazo precisa ser uma data futura.");
    await waitFor(() => expect(screen.getByLabelText("Cargo-alvo")).toHaveValue("Backend"));
  });
});
