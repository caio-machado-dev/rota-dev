import { act, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ThemeProvider } from "./theme-provider";
import { ThemeToggle } from "./theme-toggle";

function setup() {
  return render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  );
}

describe("ThemeToggle", () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.className = "dark";
    vi.stubGlobal("matchMedia", () => ({ matches: false, addEventListener: () => {}, removeEventListener: () => {} }));
  });

  it("começa no escuro quando não há preferência salva", () => {
    setup();
    expect(screen.getByRole("radio", { name: "Escuro" })).toBeChecked();
  });

  it("troca para o claro, tira a classe dark e guarda a escolha", async () => {
    setup();
    await act(async () => screen.getByRole("radio", { name: "Claro" }).click());
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    expect(localStorage.getItem("rota-theme")).toBe("light");
    expect(screen.getByRole("radio", { name: "Claro" })).toBeChecked();
  });

  it("lê a preferência salva ao montar", async () => {
    localStorage.setItem("rota-theme", "light");
    setup();
    expect(await screen.findByRole("radio", { name: "Claro", checked: true })).toBeInTheDocument();
  });
});
