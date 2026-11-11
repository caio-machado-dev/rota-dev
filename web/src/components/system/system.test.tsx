import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { Logo } from "@/components/brand/logo";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";

import { PageHeader } from "./page-header";
import { EmptyState, ErrorState } from "./states";

describe("componentes-base", () => {
  it("Progress limita o valor ao máximo e expõe os atributos ARIA", () => {
    render(<Progress label="Meta semanal" value={700} max={600} />);
    const bar = screen.getByRole("progressbar", { name: "Meta semanal" });
    expect(bar).toHaveAttribute("aria-valuenow", "600");
    expect(bar).toHaveAttribute("aria-valuemax", "600");
  });

  it("EmptyState mostra a ação útil", () => {
    render(<EmptyState title="Ainda não há plano" description="Defina um objetivo." action={<a href="/plano">Criar plano</a>} />);
    expect(screen.getByText("Ainda não há plano")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Criar plano" })).toBeInTheDocument();
  });

  it("ErrorState avisa como alerta e chama o retry", () => {
    const retry = vi.fn();
    render(<ErrorState message="Não consegui carregar." onRetry={retry} />);
    expect(screen.getByRole("alert")).toHaveTextContent("Não consegui carregar.");
    screen.getByRole("button", { name: "Tentar de novo" }).click();
    expect(retry).toHaveBeenCalledOnce();
  });

  it("PageHeader usa h1 e Logo traz o nome da marca", () => {
    render(
      <>
        <PageHeader title="Plano" description="Sua jornada" />
        <Logo />
        <Badge tone="success">Pronto</Badge>
      </>,
    );
    expect(screen.getByRole("heading", { level: 1, name: "Plano" })).toBeInTheDocument();
    expect(screen.getByText("Rota Dev")).toBeInTheDocument();
    expect(screen.getByText("Pronto")).toBeInTheDocument();
  });
});
