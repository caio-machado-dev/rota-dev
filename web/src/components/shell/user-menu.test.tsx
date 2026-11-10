import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { UserMenu } from "./user-menu";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

const me = { id: "0193f0c2-0000-7000-8000-000000000001", login: "octocat", name: "Mona", timeZone: "America/Sao_Paulo" };

afterEach(() => {
  vi.unstubAllGlobals();
  push.mockClear();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("UserMenu", () => {
  it("mostra o botão de entrar quando a API responde 401", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 401 })));

    render(<UserMenu />);

    expect(await screen.findByRole("link", { name: "Entrar com GitHub" })).toHaveAttribute(
      "href",
      "/api/oauth2/authorization/github",
    );
  });

  it("mostra o usuário logado e sai enviando o token CSRF", async () => {
    const fetchMock = vi.fn(async (path: string, init?: RequestInit) =>
      path === "/api/me" && init?.method === "GET" ? Response.json(me) : new Response(null, { status: 204 }),
    );
    vi.stubGlobal("fetch", fetchMock);
    document.cookie = "XSRF-TOKEN=token-123";

    render(<UserMenu />);
    expect(await screen.findByText("Mona")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Sair" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
    expect(await screen.findByRole("link", { name: "Entrar com GitHub" })).toBeInTheDocument();
    const logoutCall = fetchMock.mock.calls.find(([path]) => path === "/api/logout");
    expect(new Headers(logoutCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBe("token-123");
  });
});
