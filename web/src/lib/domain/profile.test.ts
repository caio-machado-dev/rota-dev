import { describe, expect, it } from "vitest";

import { diffSnapshots, latestBySource, skillRows, validateUpload } from "./profile";

const snap = (id: string, sourceType: string, createdAt: string, skills: [string, number][], evidences: { id: string; title: string; skill?: string }[] = []) => ({
  id,
  sourceType,
  createdAt,
  skills: skills.map(([name, weight]) => ({ name, weight, confidence: "HIGH" })),
  evidences: evidences.map((e) => ({ ...e, kind: "REPO", confidence: "HIGH" })),
});

describe("profile", () => {
  it("valida formato e tamanho do upload por tipo", () => {
    expect(validateUpload("RESUME", { name: "cv.PDF", size: 1000 })).toBeNull();
    expect(validateUpload("RESUME", { name: "cv.zip", size: 1000 })).toMatch(/Formato não aceito/);
    expect(validateUpload("LINKEDIN", { name: "dados.zip", size: 1000 })).toBeNull();
    expect(validateUpload("RESUME", { name: "cv.pdf", size: 11 * 1024 * 1024 })).toMatch(/10 MB/);
    expect(validateUpload("RESUME", { name: "cv.pdf", size: 0 })).toMatch(/vazio/);
  });

  it("fica com o snapshot mais recente de cada fonte", () => {
    const latest = latestBySource([snap("1", "GITHUB", "2026-10-01", []), snap("2", "GITHUB", "2026-11-01", []), snap("3", "RESUME", "2026-09-01", [])]);
    expect(latest.map((s) => s.id).sort()).toEqual(["2", "3"]);
  });

  it("liga cada habilidade às evidências que a citam", () => {
    const rows = skillRows([snap("1", "GITHUB", "2026-11-01", [["Java", 0.7], ["SQL", 0.4]], [{ id: "e1", title: "pedidos", skill: "java" }])]);
    expect(rows.find((r) => r.name === "Java")?.evidences).toHaveLength(1);
    expect(rows.find((r) => r.name === "SQL")?.evidences).toHaveLength(0);
  });

  it("compara snapshots: entrou, saiu e mudou de peso", () => {
    const a = snap("1", "GITHUB", "2026-10-01", [["Java", 0.3], ["Python", 0.8], ["SQL", 0.5]]);
    const b = snap("2", "GITHUB", "2026-11-01", [["Java", 0.6], ["SQL", 0.52], ["Docker", 0.4]]);
    expect(diffSnapshots(a, b)).toEqual({ added: ["Docker"], removed: ["Python"], changed: [{ name: "Java", from: 0.3, to: 0.6 }] });
  });
});
