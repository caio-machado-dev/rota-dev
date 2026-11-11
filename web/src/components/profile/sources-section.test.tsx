import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderScreen, stubApi } from "@/test/helpers";

import { SourcesSection } from "./sources-section";

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

const github = { id: "g1", type: "GITHUB", externalLogin: "octocat", connectedAt: "2026-11-10T11:00:00Z", lastSyncedAt: "2026-11-10T12:00:00Z" };
const profile = {
  snapshots: [{ id: "s1", sourceType: "GITHUB", createdAt: "2026-11-10T12:00:00Z", skills: [{ name: "Java", weight: 0.7, confidence: "HIGH" }], evidences: [{ id: "e1", kind: "REPO", title: "rota-dev", confidence: "HIGH" }] }],
};

describe("SourcesSection", () => {
  it("mostra o GitHub conectado com a conta, a última leitura e os dados identificados", async () => {
    stubApi({ "/api/sources": [github], "/api/sources/uploads": [], "/api/profile": profile });
    renderScreen(<SourcesSection />);

    expect(await screen.findByText("octocat")).toBeInTheDocument();
    expect(screen.getByText("Conectado")).toBeInTheDocument();
    expect(await screen.findByText("1 evidências · 1 habilidades")).toBeInTheDocument();
    expect(screen.getByText("Dados manuais")).toBeInTheDocument();
    expect(screen.getByText("Indisponível")).toBeInTheDocument();
  });

  it("sem GitHub, avisa que está desconectado", async () => {
    stubApi({ "/api/sources": [], "/api/sources/uploads": [], "/api/profile": { snapshots: [] } });
    renderScreen(<SourcesSection />);
    expect(await screen.findByText("Desconectado")).toBeInTheDocument();
  });

  it("recusa no navegador um formato inválido, sem chamar a API", async () => {
    const calls = stubApi({ "/api/sources": [github], "/api/sources/uploads": [], "/api/profile": profile });
    renderScreen(<SourcesSection />);

    const inputs = await screen.findAllByLabelText(/Escolher arquivo/);
    fireEvent.change(inputs[1], { target: { files: [new File(["x"], "cv.zip")] } });

    expect(await screen.findByRole("alert")).toHaveTextContent("Formato não aceito para Currículo");
    expect(calls).toHaveLength(0);
  });

  it("envia o currículo válido e mostra o erro da API quando recusa", async () => {
    const calls = stubApi({ "/api/sources": [github], "/api/sources/uploads": [], "/api/profile": profile }, () => Response.json({ detail: "Arquivo ilegível." }, { status: 400 }));
    renderScreen(<SourcesSection />);

    const inputs = await screen.findAllByLabelText(/Escolher arquivo/);
    fireEvent.change(inputs[1], { target: { files: [new File(["conteudo"], "cv.pdf", { type: "application/pdf" })] } });
    fireEvent.click(await screen.findByRole("button", { name: "Enviar" }));

    await waitFor(() => expect(calls.some((c) => c.path === "/api/sources/uploads" && c.method === "POST")).toBe(true));
    expect(await screen.findByRole("alert")).toHaveTextContent("Arquivo ilegível.");
  });

  it("lista os arquivos enviados com formato, tamanho e estado", async () => {
    stubApi({
      "/api/sources": [github],
      "/api/sources/uploads": [{ id: "u1", kind: "RESUME", format: "pdf", originalName: "cv-caio.pdf", sizeBytes: 204800, status: "FAILED", createdAt: "2026-11-10T12:00:00Z" }],
      "/api/profile": profile,
    });
    renderScreen(<SourcesSection />);

    expect(await screen.findByText("cv-caio.pdf")).toBeInTheDocument();
    expect(screen.getAllByText("Não consegui ler").length).toBeGreaterThan(0);
    expect(screen.getByText(/PDF · 200 KB/)).toBeInTheDocument();
  });
});
