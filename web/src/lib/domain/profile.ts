import type { Snapshot, UploadKind } from "@/lib/api/client";

export const SOURCE_LABEL: Record<string, string> = { GITHUB: "GitHub", LINKEDIN: "LinkedIn", RESUME: "Currículo", MANUAL: "Dados manuais" };
export const CONFIDENCE_LABEL: Record<string, string> = { HIGH: "Comprovado", MEDIUM: "Estruturado", LOW: "Declarado" };
export const CONFIDENCE_ORDER = ["HIGH", "MEDIUM", "LOW"] as const;

export const UPLOAD_RULES: Record<UploadKind, { label: string; extensions: string[]; hint: string }> = {
  RESUME: { label: "Currículo", extensions: [".pdf", ".docx"], hint: "PDF ou DOCX, até 10 MB" },
  LINKEDIN: { label: "LinkedIn", extensions: [".zip", ".pdf"], hint: "ZIP da exportação de dados ou PDF do perfil, até 10 MB" },
};
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

/** Valida o arquivo antes do envio, com a mesma regra da API (a API confere de novo). Devolve a mensagem de erro ou `null`. */
export function validateUpload(kind: UploadKind, file: { name: string; size: number }): string | null {
  const rule = UPLOAD_RULES[kind];
  const lower = file.name.toLowerCase();
  if (!rule.extensions.some((ext) => lower.endsWith(ext))) return `Formato não aceito para ${rule.label}. Use ${rule.extensions.join(" ou ")}.`;
  if (file.size > MAX_UPLOAD_BYTES) return "O arquivo passa de 10 MB.";
  if (file.size === 0) return "O arquivo está vazio.";
  return null;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** O snapshot mais recente de cada fonte (o que vale hoje). */
export function latestBySource(snapshots: Snapshot[]): Snapshot[] {
  const latest = new Map<string, Snapshot>();
  for (const s of snapshots) {
    const current = latest.get(s.sourceType);
    if (!current || s.createdAt > current.createdAt) latest.set(s.sourceType, s);
  }
  return [...latest.values()];
}

export type SkillRow = {
  key: string;
  name: string;
  weight: number;
  confidence: string;
  source: string;
  evidences: Snapshot["evidences"];
};

/** Habilidades dos snapshots mais recentes de cada fonte, com as evidências que citam a habilidade. */
export function skillRows(snapshots: Snapshot[]): SkillRow[] {
  return latestBySource(snapshots).flatMap((snapshot) =>
    snapshot.skills.map((skill) => ({
      key: `${snapshot.sourceType}:${skill.name}`,
      name: skill.name,
      weight: skill.weight,
      confidence: skill.confidence,
      source: snapshot.sourceType,
      evidences: snapshot.evidences.filter((e) => e.skill?.toLowerCase() === skill.name.toLowerCase()),
    })),
  );
}

export type SnapshotDiff = { added: string[]; removed: string[]; changed: { name: string; from: number; to: number }[] };

/** Diferença de habilidades entre dois snapshots (de `from` para `to`), com mudança de peso a partir de 5 pontos percentuais. */
export function diffSnapshots(from: Snapshot, to: Snapshot): SnapshotDiff {
  const a = new Map(from.skills.map((s) => [s.name, s.weight]));
  const b = new Map(to.skills.map((s) => [s.name, s.weight]));
  return {
    added: [...b.keys()].filter((n) => !a.has(n)),
    removed: [...a.keys()].filter((n) => !b.has(n)),
    changed: [...b]
      .filter(([n, w]) => a.has(n) && Math.abs(w - (a.get(n) as number)) >= 0.05)
      .map(([name, to]) => ({ name, from: a.get(name) as number, to })),
  };
}
