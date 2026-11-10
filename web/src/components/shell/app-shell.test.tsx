import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AppShell } from "./app-shell";

// Fora do Next.js não existe roteador: simulamos a rota atual.
vi.mock("next/navigation", () => ({ usePathname: () => "/plano", useRouter: () => ({ push: vi.fn() }) }));
// O menu do usuário chama /api/me; aqui ninguém está logado.
vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 401 })));

describe("AppShell", () => {
  it("renderiza a navegação no desktop e no celular, com a rota atual marcada", () => {
    render(<AppShell>conteúdo</AppShell>);

    const navs = screen.getAllByRole("navigation", { name: "Navegação principal" });
    expect(navs).toHaveLength(2);

    for (const nav of navs) {
      expect(within(nav).getByRole("link", { name: "Plano" })).toHaveAttribute("aria-current", "page");
      expect(within(nav).getByRole("link", { name: "Agora" })).not.toHaveAttribute("aria-current");
    }
    expect(screen.getByText("conteúdo")).toBeInTheDocument();
  });
});
