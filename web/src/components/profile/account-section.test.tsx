import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { AccountSection } from "./account-section";

afterEach(() => vi.unstubAllGlobals());

const me = { id: "0193f0c2-0000-7000-8000-000000000001", login: "octocat", name: "Mona", email: "mona@exemplo.com", timeZone: "America/Sao_Paulo", tier: "FREE" };

describe("AccountSection", () => {
  it("mostra a conta e oferece o download dos dados", async () => {
    stubApi({ "/api/me": me });
    renderScreen(<AccountSection />);

    expect(await screen.findByText("mona@exemplo.com")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar meus dados" })).toHaveAttribute("href", "/api/account/export");
  });

  it("só exclui depois do diálogo e do login digitado; não chama a API antes", async () => {
    const onDeleted = vi.fn();
    const calls = stubApi({ "/api/me": me }, () => new Response(null, { status: 204 }));
    renderScreen(<AccountSection onDeleted={onDeleted} />);

    fireEvent.click(await screen.findByRole("button", { name: "Excluir minha conta" }));
    const dialog = await screen.findByRole("dialog", { name: "Excluir a conta para sempre?" });
    const confirm = within(dialog).getByRole("button", { name: "Excluir definitivamente" });
    expect(confirm).toBeDisabled();
    expect(calls).toHaveLength(0);

    fireEvent.change(within(dialog).getByLabelText(/Digite octocat/), { target: { value: "octocat" } });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);

    await waitFor(() => expect(onDeleted).toHaveBeenCalled());
    expect(calls[0]).toMatchObject({ method: "DELETE", path: "/api/account", body: { confirmLogin: "octocat" } });
  });

  it("mostra a mensagem da API quando a exclusão falha", async () => {
    stubApi({ "/api/me": me }, () => Response.json({ detail: "O login não confere." }, { status: 400 }));
    renderScreen(<AccountSection onDeleted={vi.fn()} />);

    fireEvent.click(await screen.findByRole("button", { name: "Excluir minha conta" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/Digite octocat/), { target: { value: "octocat" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Excluir definitivamente" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("O login não confere.");
  });
});
