"use client";

import { Briefcase, FileText, GitBranch, PenLine, RefreshCw } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useSWRConfig } from "swr";

import { FileUploader } from "@/components/evidence/file-uploader";
import { ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { ApiError, syncGithub, uploadDocument, type Source, type Upload, type UploadKind } from "@/lib/api/client";
import { keys, useProfile, useSources, useUploads } from "@/lib/api/hooks";
import { formatBytes, latestBySource, UPLOAD_RULES, validateUpload } from "@/lib/domain/profile";
import { cn } from "@/lib/utils";

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const UPLOAD_STATUS: Record<string, { label: string; tone: "info" | "success" | "danger" }> = {
  RECEIVED: { label: "Lendo…", tone: "info" },
  PROCESSED: { label: "Lido", tone: "success" },
  FAILED: { label: "Não consegui ler", tone: "danger" },
};

function GithubCard({ source }: { source: Source | undefined }) {
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const { data: profile } = useProfile();
  const [syncingSince, setSyncingSince] = useState<string | null>(null);
  const started = useRef(0);

  // Depois de pedir a leitura (job), consulta as fontes a cada 3 s até `lastSyncedAt` mudar (ou 90 s).
  useEffect(() => {
    if (!syncingSince) return;
    const id = setInterval(() => {
      void mutate(keys.sources);
      if (Date.now() - started.current > 90_000) setSyncingSince(null);
    }, 3000);
    return () => clearInterval(id);
  }, [syncingSince, mutate]);

  const syncing = syncingSince !== null && (source?.lastSyncedAt ?? "") <= syncingSince;
  useEffect(() => {
    if (syncingSince !== null && !syncing) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- a leitura terminou: libera o botão e avisa.
      setSyncingSince(null);
      void mutate(keys.profile);
      toast.success("GitHub atualizado.");
    }
  }, [syncing, syncingSince, mutate, toast]);

  const snapshot = latestBySource(profile?.snapshots ?? []).find((s) => s.sourceType === "GITHUB");
  const repos = snapshot?.evidences.length ?? 0;

  async function sync() {
    try {
      await syncGithub();
      started.current = Date.now();
      setSyncingSince(source?.lastSyncedAt ?? "");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Não consegui pedir a atualização do GitHub.");
    }
  }

  return (
    <Card aria-labelledby="src-github">
      <CardHeader>
        <div className="flex items-center gap-2">
          <GitBranch className="size-5 text-muted-foreground" strokeWidth={1.5} aria-hidden />
          <CardTitle id="src-github">GitHub</CardTitle>
        </div>
        {source ? <Badge tone="success">Conectado</Badge> : <Badge tone="warning">Desconectado</Badge>}
      </CardHeader>
      {source ? (
        <>
          <dl className="grid gap-x-6 gap-y-2 text-small sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Conta</dt>
              <dd className="font-mono">{source.externalLogin}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Conectado em</dt>
              <dd className="num">{dateFormat.format(new Date(source.connectedAt))}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Última leitura</dt>
              <dd className="num">{source.lastSyncedAt ? dateFormat.format(new Date(source.lastSyncedAt)) : "Primeira leitura em andamento"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Dados identificados</dt>
              <dd className="num">
                {snapshot ? `${repos} evidências · ${snapshot.skills.length} habilidades` : "Nada ainda"}
              </dd>
            </div>
          </dl>
          <Button variant="outline" size="sm" className="mt-4" disabled={syncing} onClick={() => void sync()}>
            <RefreshCw className={cn(syncing && "animate-spin")} aria-hidden />
            {syncing ? "Lendo o GitHub…" : "Atualizar agora"}
          </Button>
        </>
      ) : (
        <p className="text-small text-muted-foreground">O GitHub se conecta ao entrar. Saia e entre de novo com o GitHub para reconectar.</p>
      )}
    </Card>
  );
}

function UploadCard({ kind, uploads }: { kind: UploadKind; uploads: Upload[] }) {
  const { mutate } = useSWRConfig();
  const toast = useToast();
  const rule = UPLOAD_RULES[kind];
  const mine = uploads.filter((u) => u.kind === kind).sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  const Icon = kind === "LINKEDIN" ? Briefcase : FileText;

  return (
    <Card aria-labelledby={`src-${kind}`}>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Icon className="size-5 text-muted-foreground" strokeWidth={1.5} aria-hidden />
          <CardTitle id={`src-${kind}`}>{rule.label}</CardTitle>
        </div>
        {mine.length > 0 ? <Badge tone={UPLOAD_STATUS[mine[0].status]?.tone ?? "neutral"}>{UPLOAD_STATUS[mine[0].status]?.label ?? mine[0].status}</Badge> : <Badge>Sem arquivo</Badge>}
      </CardHeader>
      <FileUploader
        accept={rule.extensions}
        hint={rule.hint}
        validate={(file) => validateUpload(kind, file)}
        onUpload={async (file) => {
          try {
            await uploadDocument(kind, file);
          } catch (e) {
            throw new Error(e instanceof ApiError ? e.message : "Falha no envio. Tente de novo.");
          }
          await Promise.all([mutate(keys.uploads), mutate(keys.profile)]);
          toast.success(`${file.name} enviado. A leitura roda em segundo plano.`);
        }}
      />
      {mine.length > 0 ? (
        <ul className="mt-4 divide-y divide-border border-t border-border">
          {mine.map((u) => (
            <li key={u.id} className="flex items-center gap-3 py-2 text-small">
              <span className="min-w-0 flex-1 truncate">{u.originalName}</span>
              <span className="num text-micro text-muted-foreground">
                {u.format.toUpperCase()} · {formatBytes(u.sizeBytes)} · {dateFormat.format(new Date(u.createdAt))}
              </span>
              <Badge tone={UPLOAD_STATUS[u.status]?.tone ?? "neutral"}>{UPLOAD_STATUS[u.status]?.label ?? u.status}</Badge>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}

/** Aba "Fontes": GitHub (conexão e leitura), LinkedIn e currículo (envio de arquivo) e dados manuais (ainda sem suporte na API). */
export function SourcesSection() {
  const { data: sources, error, isLoading, mutate } = useSources();
  const { data: uploads } = useUploads();

  if (isLoading) return <Skeleton className="h-96" />;
  if (error || !sources) return <ErrorState message="Não consegui carregar as fontes." onRetry={() => void mutate()} />;

  return (
    <div className="grid items-start gap-6 lg:grid-cols-2">
      <GithubCard source={sources.find((s) => s.type === "GITHUB")} />
      <UploadCard kind="LINKEDIN" uploads={uploads ?? []} />
      <UploadCard kind="RESUME" uploads={uploads ?? []} />
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <PenLine className="size-5 text-muted-foreground" strokeWidth={1.5} aria-hidden />
            <CardTitle>Dados manuais</CardTitle>
          </div>
          <Badge>Indisponível</Badge>
        </CardHeader>
        <CardDescription>A API ainda não aceita habilidades e experiências digitadas à mão. Por enquanto, o perfil vem do GitHub e dos arquivos.</CardDescription>
      </Card>
    </div>
  );
}
