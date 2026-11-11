import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { OnboardingWizard } from "./onboarding-wizard";

const push = vi.fn();
const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace }) }));

afterEach(() => {
  vi.unstubAllGlobals();
  push.mockClear();
  replace.mockClear();
});

const me = { id: "0193f0c2-0000-7000-8000-000000000001", login: "octocat", name: "Mona", timeZone: "America/Sao_Paulo", tier: "FREE" };
const github = { id: "g1", type: "GITHUB", externalLogin: "octocat", connectedAt: "2026-11-10T11:00:00Z", lastSyncedAt: "2026-11-10T12:00:00Z" };
const profile = {
  snapshots: [{ id: "s1", sourceType: "GITHUB", createdAt: "2026-11-10T12:00:00Z", skills: [{ name: "Python", weight: 0.8, confidence: "HIGH" }], evidences: [{ id: "e1", kind: "REPO", title: "x", confidence: "HIGH" }] }],
};
const base = { "/api/me": me, "/api/sources": [github], "/api/sources/uploads": [], "/api/profile": profile, "/api/goal": null, "/api/diagnosis/latest": null };

describe("OnboardingWizard", () => {
  it("manda para /login quando não há sessão", async () => {
    stubApi({ "/api/me": () => new Response(null, { status: 401 }) });
    renderScreen(<OnboardingWizard />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("percorre as quatro etapas: fontes, perfil, objetivo e diagnóstico", async () => {
    const calls = stubApi(base, (call) => (call.path === "/api/goal" ? Response.json(call.body) : Response.json({ id: "d1" }, { status: 202 })));
    renderScreen(<OnboardingWizard />);

    // 1. fontes: GitHub conectado libera o botão
    expect(await screen.findByRole("heading", { level: 1, name: "Conectar fontes" })).toBeInTheDocument();
    const next = screen.getByRole("button", { name: /Continuar/ });
    await waitFor(() => expect(next).toBeEnabled());
    fireEvent.click(next);

    // 2. perfil encontrado
    expect(await screen.findByRole("heading", { level: 1, name: "Revisar perfil" })).toBeInTheDocument();
    expect(await screen.findByText("Python")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Continuar/ }));

    // 3. objetivo: salvar avança sozinho
    expect(await screen.findByRole("heading", { level: 1, name: "Definir objetivo" })).toBeInTheDocument();
    fireEvent.change(await screen.findByLabelText("Cargo-alvo"), { target: { value: "Backend Java" } });
    fireEvent.change(screen.getByLabelText("Prazo"), { target: { value: "2027-06-30" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar objetivo" }));

    // 4. diagnóstico
    expect(await screen.findByRole("heading", { level: 1, name: "Gerar diagnóstico" })).toBeInTheDocument();
    expect(await screen.findByText(/Cargo-alvo: Backend Java/)).toBeInTheDocument();
    const generate = screen.getByRole("button", { name: "Gerar diagnóstico" });
    await waitFor(() => expect(generate).toBeEnabled());
    fireEvent.click(generate);
    await waitFor(() => expect(calls.some((c) => c.path === "/api/diagnosis" && c.method === "POST")).toBe(true));
  });

  it("sem perfil lido ainda, a etapa de revisão avisa que está lendo", async () => {
    stubApi({ ...base, "/api/profile": { snapshots: [] } });
    renderScreen(<OnboardingWizard />);

    await waitFor(() => expect(screen.getByRole("button", { name: /Continuar/ })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: /Continuar/ }));
    expect(await screen.findByText(/Ainda estamos lendo as suas fontes/)).toBeInTheDocument();
  });

  it("com o diagnóstico pronto, oferece ir direto para ele", async () => {
    stubApi({ ...base, "/api/goal": { targetRole: "Backend", targetLevel: "PLENO", track: "BACKEND", deadline: "2027-06-30", hoursPerWeek: 8, wantsCertifications: false }, "/api/diagnosis/latest": { id: "d1", status: "READY", createdAt: "2026-11-10T12:00:00Z", axes: [], gaps: [] } });
    renderScreen(<OnboardingWizard />);

    await waitFor(() => expect(screen.getByRole("button", { name: /Continuar/ })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: /Continuar/ })); // perfil
    fireEvent.click(await screen.findByRole("button", { name: /Continuar/ })); // objetivo
    fireEvent.click(await screen.findByRole("button", { name: /Já salvei/ })); // diagnóstico
    fireEvent.click(await screen.findByRole("button", { name: /Ver o diagnóstico/ }));
    expect(push).toHaveBeenCalledWith("/plano/diagnostico");
  });
});
