import type { components } from "./schema";

/** Tipos gerados do contrato da API (`pnpm api:types`, ADR 0010). Nunca escreva estes tipos à mão. */
export type Me = components["schemas"]["MeResponse"];
export type Source = components["schemas"]["SourceResponse"];
export type Upload = components["schemas"]["UploadResponse"];
export type Profile = components["schemas"]["ProfileResponse"];
export type Snapshot = components["schemas"]["SnapshotResponse"];
export type UploadKind = "LINKEDIN" | "RESUME";
export type Goal = components["schemas"]["GoalResponse"];
export type GoalInput = components["schemas"]["GoalRequest"];
export type Diagnosis = components["schemas"]["DiagnosisResponse"];
export type PlanVersion = components["schemas"]["PlanResponse"];

/** Onde o login começa. É um link comum (navegação inteira), não uma chamada `fetch`. */
export const LOGIN_URL = "/api/oauth2/authorization/github";

/**
 * Lê o token CSRF que a API grava no cookie `XSRF-TOKEN` (ADR 0005). Ele vai de volta no
 * cabeçalho `X-XSRF-TOKEN` de toda requisição que altera dados.
 */
export function readCsrfToken(cookie: string = document.cookie): string | undefined {
  const match = cookie.split("; ").find((part) => part.startsWith("XSRF-TOKEN="));
  return match ? decodeURIComponent(match.slice("XSRF-TOKEN=".length)) : undefined;
}

/** `fetch` para a API na mesma origem, já com o cabeçalho CSRF quando o método altera dados. */
export async function apiFetch(path: `/api/${string}`, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (method !== "GET" && method !== "HEAD") {
    const token = readCsrfToken();
    if (token) headers.set("X-XSRF-TOKEN", token);
  }
  return fetch(path, { ...init, method, headers, credentials: "same-origin" });
}

/** O usuário logado, ou `null` se não houver sessão (a API responde 401). */
export async function getMe(): Promise<Me | null> {
  const response = await apiFetch("/api/me");
  if (response.status === 401) return null;
  if (!response.ok) throw new Error(`GET /api/me respondeu ${response.status}`);
  return (await response.json()) as Me;
}

export async function logout(): Promise<void> {
  const response = await apiFetch("/api/logout", { method: "POST" });
  if (!response.ok) throw new Error(`POST /api/logout respondeu ${response.status}`);
}

/** Erro de uma chamada à API, com a mensagem do `ProblemDetail` quando houver. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

async function json<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
    throw new ApiError(response.status, problem?.detail ?? `A API respondeu ${response.status}`);
  }
  return (await response.json()) as T;
}

export async function listSources(): Promise<Source[]> {
  return json(await apiFetch("/api/sources"));
}

export async function listUploads(): Promise<Upload[]> {
  return json(await apiFetch("/api/sources/uploads"));
}

export async function getProfile(): Promise<Profile> {
  return json(await apiFetch("/api/profile"));
}

/** Pede uma nova leitura do GitHub. A API responde 202 e o trabalho roda num job. */
export async function syncGithub(): Promise<void> {
  const response = await apiFetch("/api/sources/github/sync", { method: "POST" });
  if (!response.ok) throw new ApiError(response.status, "Não consegui pedir a atualização do GitHub.");
}

/**
 * Envia o arquivo como `multipart/form-data`. Não defina o Content-Type à mão: o navegador
 * monta o cabeçalho com o `boundary` que separa os campos.
 */
export async function uploadDocument(kind: UploadKind, file: File): Promise<Upload> {
  const form = new FormData();
  form.append("kind", kind);
  form.append("file", file);
  return json(await apiFetch("/api/sources/uploads", { method: "POST", body: form }));
}

/** O objetivo de carreira, ou `null` se o usuário ainda não definiu um (a API responde 404). */
export async function getGoal(): Promise<Goal | null> {
  const response = await apiFetch("/api/goal");
  if (response.status === 404) return null;
  return json(response);
}

export async function saveGoal(goal: GoalInput): Promise<Goal> {
  return json(
    await apiFetch("/api/goal", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(goal),
    }),
  );
}

/** O diagnóstico mais recente, ou `null` se o usuário nunca pediu um. */
export async function getLatestDiagnosis(): Promise<Diagnosis | null> {
  const response = await apiFetch("/api/diagnosis/latest");
  if (response.status === 404) return null;
  return json(response);
}

/** Pede um diagnóstico novo. A API responde 202 e gera num job; consulte `getLatestDiagnosis`. */
export async function requestDiagnosis(): Promise<void> {
  await json(await apiFetch("/api/diagnosis", { method: "POST" }));
}

/** A versão mais recente do plano (pode estar sendo gerada), ou `null` se nunca houve uma. */
export async function getLatestPlan(): Promise<PlanVersion | null> {
  const response = await apiFetch("/api/plan/latest");
  if (response.status === 404) return null;
  return json(response);
}

export async function listPlanVersions(): Promise<PlanVersion[]> {
  return json(await apiFetch("/api/plan/versions"));
}

/** Pede uma versão nova do plano. A API responde 202 e gera num job. */
export async function requestPlan(): Promise<void> {
  await json(await apiFetch("/api/plan", { method: "POST" }));
}
