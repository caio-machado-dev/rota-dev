"use client";

import { ChevronDown } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useMemo, useState } from "react";

import { EvidenceChip } from "@/components/evidence/evidence-chip";
import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, Select } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { useProfile } from "@/lib/api/hooks";
import { CONFIDENCE_LABEL, CONFIDENCE_ORDER, diffSnapshots, skillRows, SOURCE_LABEL, type SkillRow } from "@/lib/domain/profile";
import { cn } from "@/lib/utils";

const CONFIDENCE_TONE: Record<string, "success" | "info" | "warning"> = { HIGH: "success", MEDIUM: "info", LOW: "warning" };
const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

function SkillItem({ row }: { row: SkillRow }) {
  const [open, setOpen] = useState(false);
  return (
    <li>
      <button
        type="button"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        className="grid w-full grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1 rounded-control px-3 py-2.5 text-left outline-none hover:bg-accent/50 focus-visible:ring-2 focus-visible:ring-ring sm:grid-cols-[minmax(0,12rem)_1fr_auto_auto_auto]"
      >
        <span className="truncate text-body font-medium">{row.name}</span>
        <span className="col-span-2 order-last sm:order-none sm:col-span-1">
          <Progress label={`Peso de ${row.name}: ${Math.round(row.weight * 100)}%`} value={Math.round(row.weight * 100)} />
        </span>
        <Badge tone={CONFIDENCE_TONE[row.confidence] ?? "neutral"}>{CONFIDENCE_LABEL[row.confidence] ?? row.confidence}</Badge>
        <span className="hidden text-small text-muted-foreground sm:inline">{SOURCE_LABEL[row.source] ?? row.source}</span>
        <ChevronDown className={cn("size-4 text-muted-foreground transition-transform duration-150", open && "rotate-180")} strokeWidth={1.5} aria-hidden />
      </button>
      {open ? (
        <div className="mb-2 ml-3 border-l border-border pl-4 text-small">
          <p className="text-muted-foreground">
            Peso <span className="num text-foreground">{Math.round(row.weight * 100)}%</span> · fonte {SOURCE_LABEL[row.source] ?? row.source}
          </p>
          {row.evidences.length > 0 ? (
            <ul className="mt-2 flex flex-wrap gap-2">
              {row.evidences.map((e) => (
                <li key={e.id} className="max-w-full">
                  <EvidenceChip title={e.title} url={e.url} />
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-1 text-muted-foreground">Nenhuma evidência específica citada para esta habilidade.</p>
          )}
        </div>
      ) : null}
    </li>
  );
}

function Compare({ snapshots }: { snapshots: NonNullable<ReturnType<typeof useProfile>["data"]>["snapshots"] }) {
  const bySource = Object.groupBy(snapshots, (s) => s.sourceType);
  const comparable = Object.entries(bySource).filter(([, list]) => (list?.length ?? 0) >= 2);
  const [source, setSource] = useState(comparable[0]?.[0] ?? "");
  if (comparable.length === 0) return <p className="text-small text-muted-foreground">A comparação aparece quando uma fonte tiver duas leituras.</p>;

  const list = [...(bySource[source] ?? [])].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  const from = list[list.length - 2];
  const to = list[list.length - 1];
  const diff = from && to ? diffSnapshots(from, to) : null;

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center gap-3">
        <Select aria-label="Fonte a comparar" value={source} onChange={(e) => setSource(e.target.value)} className="w-44">
          {comparable.map(([s]) => (
            <option key={s} value={s}>
              {SOURCE_LABEL[s] ?? s}
            </option>
          ))}
        </Select>
        {from && to ? (
          <span className="num text-small text-muted-foreground">
            {dateFormat.format(new Date(from.createdAt))} → {dateFormat.format(new Date(to.createdAt))}
          </span>
        ) : null}
      </div>
      {diff && diff.added.length + diff.removed.length + diff.changed.length === 0 ? <p className="text-small text-muted-foreground">Nada mudou entre as duas leituras.</p> : null}
      <ul className="overflow-hidden rounded-control border border-border font-mono text-small empty:hidden">
        {diff?.removed.map((n) => (
          <li key={`-${n}`} className="flex gap-2 bg-destructive/10 px-3 py-1">
            <span aria-label="saiu" className="text-destructive">−</span> {n}
          </li>
        ))}
        {diff?.added.map((n) => (
          <li key={`+${n}`} className="flex gap-2 bg-success/10 px-3 py-1">
            <span aria-label="entrou" className="text-success">+</span> {n}
          </li>
        ))}
        {diff?.changed.map((c) => (
          <li key={`~${c.name}`} className="flex gap-2 bg-info/10 px-3 py-1">
            <span aria-label="mudou" className="text-info">~</span> {c.name}: {Math.round(c.from * 100)}% → {Math.round(c.to * 100)}%
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * Aba "O que encontramos": habilidades de cada fonte (leitura mais recente), com confiança, peso, fonte e as
 * evidências. Filtros por fonte, confiança e nome (a paleta de comandos abre aqui com `?q=`). Confirmar, corrigir,
 * remover ou adicionar à mão ainda não existe na API, então a lista é só de leitura.
 */
export function FoundSection() {
  const { data: profile, error, isLoading, mutate } = useProfile();
  const params = useSearchParams();
  const [query, setQuery] = useState(params.get("q") ?? "");
  const [source, setSource] = useState("");
  const [confidence, setConfidence] = useState("");

  const rows = useMemo(() => skillRows(profile?.snapshots ?? []), [profile]);
  const filtered = rows
    .filter((r) => (!source || r.source === source) && (!confidence || r.confidence === confidence) && r.name.toLowerCase().includes(query.trim().toLowerCase()))
    .sort((a, b) => b.weight - a.weight);
  const sources = [...new Set(rows.map((r) => r.source))];

  if (isLoading) return <Skeleton className="h-96" />;
  if (error || !profile) return <ErrorState message="Não consegui carregar o perfil." onRetry={() => void mutate()} />;
  if (rows.length === 0) {
    return (
      <Card>
        <EmptyState title="Ainda não encontramos nada" description="A leitura do GitHub e dos arquivos roda em segundo plano. Conecte uma fonte ou envie um arquivo em Fontes." />
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Habilidades identificadas</CardTitle>
            <CardDescription>
              {filtered.length} de {rows.length}. Clique numa habilidade para ver as evidências.
            </CardDescription>
          </div>
        </CardHeader>
        <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_10rem_10rem]">
          <Input type="search" aria-label="Buscar habilidade" placeholder="Buscar habilidade…" value={query} onChange={(e) => setQuery(e.target.value)} />
          <Select aria-label="Filtrar por fonte" value={source} onChange={(e) => setSource(e.target.value)}>
            <option value="">Todas as fontes</option>
            {sources.map((s) => (
              <option key={s} value={s}>
                {SOURCE_LABEL[s] ?? s}
              </option>
            ))}
          </Select>
          <Select aria-label="Filtrar por confiança" value={confidence} onChange={(e) => setConfidence(e.target.value)}>
            <option value="">Toda confiança</option>
            {CONFIDENCE_ORDER.map((c) => (
              <option key={c} value={c}>
                {CONFIDENCE_LABEL[c]}
              </option>
            ))}
          </Select>
        </div>
        {filtered.length === 0 ? <p className="py-6 text-center text-small text-muted-foreground">Nenhuma habilidade com esses filtros.</p> : <ul className="divide-y divide-border">{filtered.map((r) => <SkillItem key={r.key} row={r} />)}</ul>}
        <p className="mt-4 border-t border-border pt-3 text-micro text-muted-foreground">
          Confirmar, corrigir, remover ou adicionar habilidades ainda não é possível: a API não oferece essas operações. Comprovado = veio de evidência
          (repositórios), estruturado = de arquivo estruturado, declarado = dito pela própria pessoa.
        </p>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Comparar leituras</CardTitle>
            <CardDescription>O que mudou entre as duas leituras mais recentes de uma fonte.</CardDescription>
          </div>
        </CardHeader>
        <Compare snapshots={profile.snapshots} />
      </Card>
    </div>
  );
}
