import { describe, expect, it } from "vitest";

import { breadcrumb, isActive } from "./navigation";

describe("isActive", () => {
  it("marca a raiz só quando a rota é exatamente /", () => {
    expect(isActive("/", "/")).toBe(true);
    expect(isActive("/plano", "/")).toBe(false);
  });

  it("marca a seção também nas subpáginas", () => {
    expect(isActive("/plano", "/plano")).toBe(true);
    expect(isActive("/plano/semana/3", "/plano")).toBe(true);
  });

  it("não confunde seções com prefixo parecido", () => {
    expect(isActive("/planos", "/plano")).toBe(false);
  });
});

describe("breadcrumb", () => {
  it("a raiz é Agora", () => {
    expect(breadcrumb("/")).toEqual([{ href: "/", label: "Agora" }]);
  });

  it("monta as migalhas de uma subrota", () => {
    expect(breadcrumb("/plano/diagnostico")).toEqual([
      { href: "/plano", label: "Plano" },
      { href: "/plano/diagnostico", label: "Diagnóstico" },
    ]);
  });
});
