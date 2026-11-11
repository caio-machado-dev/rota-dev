import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { SWRConfig } from "swr";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ThemeProvider } from "@/components/theme/theme-provider";

import { AppShell } from "./app-shell";

const push = vi.fn();
const replace = vi.fn();
let pathname = "/plano";
// Fora do Next.js não existe roteador: simulamos a rota atual e a navegação.
vi.mock("next/navigation", () => ({ usePathname: () => pathname, useRouter: () => ({ push, replace }) }));

const me = { id: "0193f0c2-0000-7000-8000-000000000001", login: "octocat", name: "Mona", timeZone: "America/Sao_Paulo", tier: "FREE" };

function stubApi(signedIn: boolean) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string) => {
      if (!signedIn) return new Response(null, { status: 401 });
      if (path === "/api/me") return Response.json(me);
      if (path === "/api/readiness") return Response.json({ current: { score: 42, skills: 0, gates: 0, study: 0, habit: 0 }, history: [] });
      return new Response(null, { status: 404 });
    }),
  );
}

function renderShell() {
  window.matchMedia ??= (() => ({ matches: false, addEventListener() {}, removeEventListener() {} })) as unknown as typeof window.matchMedia;
  return render(
    <SWRConfig value={{ provider: () => new Map() }}>
      <ThemeProvider>
        <AppShell>conteúdo</AppShell>
      </ThemeProvider>
    </SWRConfig>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
  push.mockClear();
  replace.mockClear();
  pathname = "/plano";
});

describe("AppShell", () => {
  it("renderiza a navegação no desktop e no celular, com a rota atual marcada", async () => {
    stubApi(true);
    renderShell();

    expect(await screen.findByText("conteúdo")).toBeInTheDocument();
    const navs = screen.getAllByRole("navigation", { name: "Navegação principal" });
    expect(navs).toHaveLength(2);
    for (const nav of navs) {
      expect(within(nav).getByRole("link", { name: "Plano" })).toHaveAttribute("aria-current", "page");
      expect(within(nav).getByRole("link", { name: "Agora" })).not.toHaveAttribute("aria-current");
    }
  });

  it("manda para /login quando não há sessão e não mostra o conteúdo", async () => {
    stubApi(false);
    renderShell();

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("conteúdo")).not.toBeInTheDocument();
  });

  it("navega com o atalho g p e abre a paleta com Ctrl+K", async () => {
    stubApi(true);
    renderShell();
    await screen.findByText("conteúdo");

    fireEvent.keyDown(window, { key: "g" });
    fireEvent.keyDown(window, { key: "r" });
    expect(push).toHaveBeenCalledWith("/progresso");

    await act(async () => {
      fireEvent.keyDown(window, { key: "k", ctrlKey: true });
    });
    expect(await screen.findByRole("dialog", { name: "Paleta de comandos" })).toBeInTheDocument();
  });

  it("não dispara atalho enquanto a pessoa digita num campo", async () => {
    stubApi(true);
    renderShell();
    await screen.findByText("conteúdo");

    const input = document.createElement("input");
    document.body.append(input);
    fireEvent.keyDown(input, { key: "g" });
    fireEvent.keyDown(input, { key: "p" });
    expect(push).not.toHaveBeenCalled();
    input.remove();
  });
});
