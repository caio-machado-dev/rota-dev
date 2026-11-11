import { describe, expect, it } from "vitest";

import { duration, firstName, greeting, pace, readinessBand, readinessDelta } from "./format";

describe("format", () => {
  it("formata duração", () => {
    expect(duration(45)).toBe("45 min");
    expect(duration(120)).toBe("2h");
    expect(duration(150)).toBe("2h30");
  });

  it("saúda pela hora do fuso do usuário", () => {
    const noonUtc = new Date("2026-11-10T15:00:00Z"); // 12h em São Paulo
    expect(greeting(noonUtc, "America/Sao_Paulo")).toBe("Boa tarde");
    expect(greeting(new Date("2026-11-10T01:00:00Z"), "America/Sao_Paulo")).toBe("Boa noite"); // 22h do dia anterior
    expect(greeting(new Date("2026-11-10T11:00:00Z"), "America/Sao_Paulo")).toBe("Bom dia");
  });

  it("usa o primeiro nome ou o login", () => {
    expect(firstName({ name: "Caio Machado", login: "caio" })).toBe("Caio");
    expect(firstName({ name: null, login: "octocat" })).toBe("octocat");
  });

  it("avalia o ritmo da semana", () => {
    expect(pace(0, undefined, 3)).toBe("none");
    expect(pace(600, 600, 2)).toBe("ahead");
    expect(pace(250, 600, 3)).toBe("on-track"); // esperado 257
    expect(pace(30, 600, 5)).toBe("behind");
  });

  it("calcula a variação entre os dois últimos pontos, em qualquer ordem", () => {
    const history = [
      { weekStart: "2026-11-09", value: { score: 42 } },
      { weekStart: "2026-11-02", value: { score: 35 } },
    ];
    expect(readinessDelta(history)).toBe(7);
    expect(readinessDelta(history.slice(0, 1))).toBeNull();
  });

  it("nomeia a faixa de prontidão", () => {
    expect(readinessBand(42)).toBe("Em construção");
    expect(readinessBand(90)).toBe("Pronto");
  });
});
