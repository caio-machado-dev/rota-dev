import { describe, expect, it } from "vitest";

import { diffPlans } from "./plan-diff";

const base = {
  summary: "Foco em testes",
  anchorProject: { title: "API de pedidos", description: "", deliverables: ["CI verde"], evidenceIds: [] },
  resources: [],
  quarters: [
    {
      index: 1,
      title: "T1",
      focus: "",
      milestones: [{ axisKey: "testes", title: "API testada", criteria: [] }],
      weeks: [{ index: 1, theme: "Testcontainers", totalMinutes: 120, tasks: [{ kind: "STUDY", title: "Ler docs", minutes: 120 }] }],
    },
  ],
};

describe("diffPlans", () => {
  it("não aponta nada quando as versões são iguais", () => {
    expect(diffPlans(base, structuredClone(base))).toEqual([]);
  });

  it("mostra o que entrou e o que saiu por seção", () => {
    const next = structuredClone(base);
    next.quarters[0].weeks[0].tasks = [{ kind: "BUILD", title: "Endpoint de pedidos", minutes: 180 }];
    next.quarters[0].weeks[0].totalMinutes = 180;
    const diff = diffPlans(base, next);

    const tasks = diff.find((d) => d.section === "Tarefas");
    expect(tasks?.added).toEqual(["Semana 1 · Construção: Endpoint de pedidos (3h)"]);
    expect(tasks?.removed).toEqual(["Semana 1 · Estudo: Ler docs (2h)"]);
    expect(diff.find((d) => d.section === "Semanas")?.added[0]).toContain("3h");
  });
});
