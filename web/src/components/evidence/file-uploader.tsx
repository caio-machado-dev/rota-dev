"use client";

import { FileUp, X } from "lucide-react";
import { useId, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Área de envio de arquivo: clique ou arraste. Valida antes de enviar (`validate` devolve a mensagem de erro) e
 * mostra o arquivo escolhido com um botão "Enviar". A API não informa % de envio, então o estado é "Enviando…".
 */
export function FileUploader({
  accept,
  hint,
  validate,
  onUpload,
}: {
  accept: string[];
  hint: string;
  validate: (file: File) => string | null;
  onUpload: (file: File) => Promise<void>;
}) {
  const id = useId();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [pending, setPending] = useState(false);

  function choose(next: File | undefined) {
    if (!next) return;
    const problem = validate(next);
    setError(problem);
    setFile(problem ? null : next);
  }

  async function send() {
    if (!file) return;
    setPending(true);
    setError(null);
    try {
      await onUpload(file);
      setFile(null);
      if (input.current) input.current.value = "";
    } catch (e) {
      setError(e instanceof Error ? e.message : "Falha no envio. Tente de novo.");
    } finally {
      setPending(false);
    }
  }

  return (
    <div>
      <label
        htmlFor={id}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          choose(e.dataTransfer.files[0]);
        }}
        className={cn(
          "relative flex cursor-pointer flex-col items-center gap-1 rounded-card border border-dashed border-input px-4 py-6 text-center transition-colors hover:bg-accent/40 has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring",
          dragging && "border-brand bg-brand/10",
        )}
      >
        <FileUp className="size-5 text-muted-foreground" strokeWidth={1.5} aria-hidden />
        <span className="text-small font-medium">Escolher arquivo ou arrastar aqui</span>
        <span className="text-micro text-muted-foreground">{hint}</span>
        <input id={id} ref={input} type="file" accept={accept.join(",")} className="sr-only" onChange={(e) => choose(e.target.files?.[0])} />
      </label>

      {file ? (
        <div className="mt-3 flex items-center gap-2 rounded-control border border-border px-3 py-2 text-small">
          <span className="min-w-0 flex-1 truncate">{file.name}</span>
          <Button size="sm" onClick={() => void send()} disabled={pending}>
            {pending ? "Enviando…" : "Enviar"}
          </Button>
          <Button size="icon-sm" variant="ghost" aria-label="Remover arquivo" disabled={pending} onClick={() => setFile(null)}>
            <X strokeWidth={1.5} aria-hidden />
          </Button>
        </div>
      ) : null}
      {error ? (
        <p role="alert" className="mt-2 text-small text-destructive">
          {error}
        </p>
      ) : null}
    </div>
  );
}
