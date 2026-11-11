import type { PlanVersion } from "@/lib/api/client";
import { duration, KIND_LABEL } from "@/lib/format";

export type DiffSection = { section: string; added: string[]; removed: string[] };

type Content = NonNullable<PlanVersion["content"]>;

/** Transforma o plano em linhas de texto agrupadas por seção, para comparar versões. */
function toLines(content: Content): Record<string, string[]> {
  const gates: string[] = [];
  const weeks: string[] = [];
  const tasks: string[] = [];
  for (const q of content.quarters) {
    for (const m of q.milestones) gates.push(`T${q.index} · ${m.title}`);
    for (const w of q.weeks) {
      weeks.push(`Semana ${w.index} · ${w.theme} (${duration(w.totalMinutes)})`);
      for (const t of w.tasks) tasks.push(`Semana ${w.index} · ${KIND_LABEL[t.kind] ?? t.kind}: ${t.title} (${duration(t.minutes)})`);
    }
  }
  return {
    Resumo: [content.summary],
    "Projeto-âncora": [content.anchorProject.title, ...content.anchorProject.deliverables.map((d) => `Entregável: ${d}`)],
    Portões: gates,
    Semanas: weeks,
    Tarefas: tasks,
    Recursos: content.resources.map((r) => `${r.title} (${r.kind})`),
  };
}

/**
 * Diferença entre duas versões do plano, calculada no cliente (a API só devolve versões inteiras).
 * Por seção: o que existe só na versão nova (`added`) e só na antiga (`removed`). Seções iguais são omitidas.
 */
export function diffPlans(from: Content, to: Content): DiffSection[] {
  const a = toLines(from);
  const b = toLines(to);
  const sections: DiffSection[] = [];
  for (const section of Object.keys(b)) {
    const before = new Set(a[section] ?? []);
    const after = new Set(b[section]);
    const added = b[section].filter((line) => !before.has(line));
    const removed = (a[section] ?? []).filter((line) => !after.has(line));
    if (added.length || removed.length) sections.push({ section, added, removed });
  }
  return sections;
}
