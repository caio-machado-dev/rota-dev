import type { components } from "./schema";

/** Tipos gerados do contrato da API (`pnpm api:types`, ADR 0010). Nunca escreva estes tipos à mão. */
export type Me = components["schemas"]["MeResponse"];

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
