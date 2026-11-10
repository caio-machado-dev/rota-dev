import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccountData } from "./account-data";

afterEach(() => vi.unstubAllGlobals());

describe("AccountData", () => {
  it("oferece o download dos dados", () => {
    render(<AccountData onDeleted={vi.fn()} />);

    expect(screen.getByRole("link", { name: "Baixar meus dados" })).toHaveAttribute("href", "/api/account/export");
  });

  it("só exclui depois de digitar o login e avisa quando não confere", async () => {
    const fetchMock = vi.fn(async (_path: string, init?: RequestInit) =>
      JSON.parse(String(init?.body)).confirmLogin === "octocat"
        ? new Response(null, { status: 204 })
        : Response.json({ detail: "Para confirmar, digite exatamente o seu login do GitHub." }, { status: 400 }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const onDeleted = vi.fn();
    render(<AccountData onDeleted={onDeleted} />);

    const button = screen.getByRole("button", { name: "Excluir minha conta" });
    expect(button).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/Digite o seu login/), { target: { value: "outro" } });
    fireEvent.click(button);
    expect(await screen.findByRole("alert")).toHaveTextContent("digite exatamente");
    expect(onDeleted).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/Digite o seu login/), { target: { value: "octocat" } });
    fireEvent.click(button);
    await waitFor(() => expect(onDeleted).toHaveBeenCalled());
    expect(fetchMock).toHaveBeenLastCalledWith("/api/account", expect.objectContaining({ method: "DELETE" }));
  });
});
