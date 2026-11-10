import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { GoalForm } from "./goal-form";

afterEach(() => vi.unstubAllGlobals());

describe("GoalForm", () => {
  it("preenche com o objetivo salvo e envia as alterações em PUT", async () => {
    const saved = {
      targetRole: "Backend Java",
      targetLevel: "PLENO",
      track: "BACKEND",
      deadline: "2027-06-30",
      hoursPerWeek: 10,
      wantsCertifications: false,
    };
    const fetchMock = vi.fn(async (_path: string, init?: RequestInit) =>
      init?.method === "PUT" ? Response.json(JSON.parse(String(init.body))) : Response.json(saved),
    );
    vi.stubGlobal("fetch", fetchMock);
    const onSaved = vi.fn();

    render(<GoalForm onSaved={onSaved} />);
    const role = (await screen.findByLabelText("Cargo-alvo")) as HTMLInputElement;
    expect(role.value).toBe("Backend Java");

    fireEvent.change(screen.getByLabelText("Horas por semana"), { target: { value: "12" } });
    fireEvent.submit(role.form!);

    expect(await screen.findByText("Salvo.")).toBeInTheDocument();
    const put = fetchMock.mock.calls.find(([, init]) => init?.method === "PUT");
    expect(JSON.parse(String(put?.[1]?.body))).toMatchObject({ targetRole: "Backend Java", hoursPerWeek: 12, track: "BACKEND" });
    expect(onSaved).toHaveBeenCalled();
  });

  it("mostra a mensagem da API quando o prazo é recusado", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (_path: string, init?: RequestInit) =>
        init?.method === "PUT"
          ? Response.json({ detail: "O prazo precisa ser uma data futura." }, { status: 400 })
          : new Response(null, { status: 404 }),
      ),
    );

    render(<GoalForm />);
    const role = (await screen.findByLabelText("Cargo-alvo")) as HTMLInputElement;
    fireEvent.change(role, { target: { value: "Backend" } });
    fireEvent.change(screen.getByLabelText("Prazo"), { target: { value: "2020-01-01" } });
    fireEvent.submit(role.form!);

    expect(await screen.findByRole("alert")).toHaveTextContent("O prazo precisa ser uma data futura.");
  });
});
