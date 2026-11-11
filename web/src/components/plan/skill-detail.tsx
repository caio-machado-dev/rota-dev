import { EvidenceChip, Unverified } from "@/components/evidence/evidence-chip";
import { Card } from "@/components/ui/card";
import type { Diagnosis } from "@/lib/api/client";

type Axis = Diagnosis["axes"][number];

/** Detalhe de uma competência: nota atual × exigida, justificativa, confiança e as evidências que sustentam a nota. */
export function SkillDetail({ axis }: { axis: Axis }) {
  const gap = Math.max(0, axis.required - axis.score);
  return (
    <Card aria-live="polite" aria-label={`Detalhe: ${axis.name}`}>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-h3 font-semibold">{axis.name}</h3>
        {axis.verified ? <span className="text-micro text-success">Verificado por evidências</span> : <Unverified />}
      </div>
      <dl className="mt-4 grid grid-cols-3 gap-4 text-small">
        <div>
          <dt className="text-muted-foreground">Nota atual</dt>
          <dd className="num text-h2 font-semibold">{axis.score}<span className="text-small font-normal text-muted-foreground"> /5</span></dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Exigida</dt>
          <dd className="num text-h2 font-semibold">{axis.required}<span className="text-small font-normal text-muted-foreground"> /5</span></dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Falta</dt>
          <dd className="num text-h2 font-semibold">{gap}</dd>
        </div>
      </dl>
      {axis.rationale ? (
        <p className="mt-4 text-body">
          <span className="text-muted-foreground">Justificativa: </span>
          {axis.rationale}
        </p>
      ) : null}
      <div className="mt-4">
        <p className="mb-2 text-small font-medium">Evidências</p>
        {axis.evidences.length > 0 ? (
          <ul className="flex flex-wrap gap-2">
            {axis.evidences.map((e) => (
              <li key={e.id} className="max-w-full">
                <EvidenceChip title={e.title} url={e.url} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-small text-muted-foreground">Nenhuma evidência encontrada para esta competência. A nota é uma estimativa.</p>
        )}
      </div>
    </Card>
  );
}
