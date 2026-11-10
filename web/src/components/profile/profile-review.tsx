"use client";

import { ExternalLink, FileUp, GitBranch, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import {
  ApiError,
  getProfile,
  listSources,
  listUploads,
  LOGIN_URL,
  syncGithub,
  uploadDocument,
  type Profile,
  type Snapshot,
  type Source,
  type Upload,
  type UploadKind,
} from "@/lib/api/client";
import { cn } from "@/lib/utils";

type Data = { sources: Source[]; uploads: Upload[]; profile: Profile };
type State = { status: "loading" } | { status: "anonymous" } | { status: "error"; message: string } | { status: "ready"; data: Data };

const SOURCE_LABEL: Record<string, string> = { GITHUB: "GitHub", LINKEDIN: "LinkedIn", RESUME: "Currículo", MANUAL: "Manual" };
const CONFIDENCE_LABEL: Record<string, string> = { HIGH: "comprovado", MEDIUM: "estruturado", LOW: "declarado" };
const UPLOAD_STATUS: Record<string, string> = { RECEIVED: "lendo…", PROCESSED: "lido", FAILED: "não consegui ler" };

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/**
 * Revisão do perfil (Fase 1): fontes conectadas, envio de arquivos e o que cada fonte mostrou,
 * sempre com a evidência ao lado. Todas as chamadas vão para `/api/...` na mesma origem.
 */
export function ProfileReview() {
  const [state, setState] = useState<State>({ status: "loading" });

  const load = useCallback(async () => {
    try {
      const [sources, uploads, profile] = await Promise.all([listSources(), listUploads(), getProfile()]);
      setState({ status: "ready", data: { sources, uploads, profile } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) setState({ status: "anonymous" });
      else setState({ status: "error", message: "Não consegui carregar o perfil. Tente de novo em instantes." });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- carga inicial vinda da API.
    void load();
  }, [load]);

  if (state.status === "loading") return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (state.status === "error") return <p className="text-sm text-destructive">{state.message}</p>;
  if (state.status === "anonymous") {
    return (
      <p className="text-sm text-muted-foreground">
        <a href={LOGIN_URL} className="font-medium text-brand hover:underline">
          Entre com GitHub
        </a>{" "}
        para montar o seu perfil.
      </p>
    );
  }

  const { sources, uploads, profile } = state.data;
  const github = sources.find((s) => s.type === "GITHUB");

  return (
    <div className="flex flex-col gap-10">
      <section aria-labelledby="fontes">
        <h2 id="fontes" className="text-sm font-semibold">
          Fontes
        </h2>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <GithubCard source={github} onSynced={load} />
          <UploadCard uploads={uploads} onUploaded={load} />
        </div>
      </section>

      <section aria-labelledby="encontrado">
        <h2 id="encontrado" className="text-sm font-semibold">
          O que encontramos
        </h2>
        {profile.snapshots.length === 0 ? (
          <p className="mt-3 text-sm text-muted-foreground">
            Nada ainda. A leitura do GitHub e dos arquivos roda em segundo plano; volte em alguns segundos.
          </p>
        ) : (
          <div className="mt-3 flex flex-col gap-6">
            {profile.snapshots.map((snapshot) => (
              <SnapshotView key={snapshot.id} snapshot={snapshot} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function GithubCard({ source, onSynced }: { source?: Source; onSynced: () => Promise<void> }) {
  const [pending, setPending] = useState(false);
  return (
    <div className="rounded-lg border border-border p-4">
      <div className="flex items-center gap-2 text-sm font-medium">
        <GitBranch className="size-4 text-brand" aria-hidden />
        GitHub
      </div>
      {source ? (
        <>
          <p className="mt-2 text-sm text-muted-foreground">
            Conectado como <span className="font-mono text-foreground">{source.externalLogin}</span>.{" "}
            {source.lastSyncedAt ? `Lido em ${dateFormat.format(new Date(source.lastSyncedAt))}.` : "Primeira leitura em andamento."}
          </p>
          <Button
            variant="outline"
            size="sm"
            className="mt-3"
            disabled={pending}
            onClick={async () => {
              setPending(true);
              try {
                await syncGithub();
                await onSynced();
              } finally {
                setPending(false);
              }
            }}
          >
            <RefreshCw className={cn(pending && "animate-spin")} aria-hidden />
            Atualizar agora
          </Button>
        </>
      ) : (
        <p className="mt-2 text-sm text-muted-foreground">Ainda não conectado. Saia e entre de novo com o GitHub.</p>
      )}
    </div>
  );
}

function UploadCard({ uploads, onUploaded }: { uploads: Upload[]; onUploaded: () => Promise<void> }) {
  const [kind, setKind] = useState<UploadKind>("RESUME");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  return (
    <div className="rounded-lg border border-border p-4">
      <div className="flex items-center gap-2 text-sm font-medium">
        <FileUp className="size-4 text-brand" aria-hidden />
        LinkedIn e currículo
      </div>
      <form
        className="mt-3 flex flex-col gap-3"
        onSubmit={async (event) => {
          event.preventDefault();
          const input = event.currentTarget.elements.namedItem("file") as HTMLInputElement;
          const file = input.files?.[0];
          if (!file) return;
          setPending(true);
          setError(null);
          try {
            await uploadDocument(kind, file);
            input.value = "";
            await onUploaded();
          } catch (e) {
            setError(e instanceof ApiError ? e.message : "Falha no envio.");
          } finally {
            setPending(false);
          }
        }}
      >
        <fieldset className="flex gap-4 text-sm">
          <legend className="sr-only">Tipo de arquivo</legend>
          {(["RESUME", "LINKEDIN"] as const).map((value) => (
            <label key={value} className="flex items-center gap-1.5">
              <input type="radio" name="kind" value={value} checked={kind === value} onChange={() => setKind(value)} />
              {value === "RESUME" ? "Currículo (PDF ou DOCX)" : "LinkedIn (ZIP ou PDF)"}
            </label>
          ))}
        </fieldset>
        <label className="text-sm">
          <span className="sr-only">Arquivo</span>
          <input name="file" type="file" accept=".pdf,.docx,.zip" required className="text-sm text-muted-foreground" />
        </label>
        <Button type="submit" size="sm" disabled={pending} className="self-start">
          {pending ? "Enviando…" : "Enviar"}
        </Button>
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}
      </form>
      {uploads.length > 0 ? (
        <ul className="mt-4 flex flex-col gap-1 border-t border-border pt-3 text-xs text-muted-foreground">
          {uploads.map((u) => (
            <li key={u.id} className="flex justify-between gap-2">
              <span className="truncate">{u.originalName}</span>
              <span className={cn(u.status === "FAILED" && "text-destructive")}>{UPLOAD_STATUS[u.status] ?? u.status}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function SnapshotView({ snapshot }: { snapshot: Snapshot }) {
  return (
    <article className="rounded-lg border border-border p-4">
      <header className="flex items-baseline justify-between gap-2">
        <h3 className="text-sm font-medium">{SOURCE_LABEL[snapshot.sourceType] ?? snapshot.sourceType}</h3>
        <span className="font-mono text-xs text-muted-foreground">{dateFormat.format(new Date(snapshot.createdAt))}</span>
      </header>

      {snapshot.skills.length > 0 ? (
        <ul className="mt-3 flex flex-col gap-1.5" aria-label="Habilidades">
          {snapshot.skills.map((skill) => (
            <li key={skill.name} className="grid grid-cols-[8rem_1fr_6rem] items-center gap-3 text-sm">
              <span className="truncate">{skill.name}</span>
              <span className="h-1.5 rounded-full bg-muted" aria-hidden>
                <span className="block h-full rounded-full bg-brand" style={{ width: `${Math.round(skill.weight * 100)}%` }} />
              </span>
              <span className="text-right font-mono text-xs text-muted-foreground">{CONFIDENCE_LABEL[skill.confidence]}</span>
            </li>
          ))}
        </ul>
      ) : null}

      {snapshot.evidences.length > 0 ? (
        <details className="mt-4">
          <summary className="cursor-pointer text-xs text-muted-foreground">Evidências ({snapshot.evidences.length})</summary>
          <ul className="mt-2 flex flex-col gap-2">
            {snapshot.evidences.map((e) => (
              <li key={e.id} className="text-sm">
                <span className="font-medium">{e.title}</span>
                {e.detail ? <span className="text-muted-foreground"> · {e.detail}</span> : null}
                {e.url ? (
                  <a href={e.url} target="_blank" rel="noreferrer" className="ml-1 inline-flex items-center text-brand" aria-label={`Abrir ${e.title}`}>
                    <ExternalLink className="size-3" aria-hidden />
                  </a>
                ) : null}
              </li>
            ))}
          </ul>
        </details>
      ) : null}
    </article>
  );
}
