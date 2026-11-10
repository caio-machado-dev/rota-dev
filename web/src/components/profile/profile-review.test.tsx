import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ProfileReview } from "./profile-review";

const profile = {
  snapshots: [
    {
      id: "s1",
      sourceType: "GITHUB",
      createdAt: "2026-11-10T12:00:00Z",
      skills: [{ name: "Java", weight: 0.7, confidence: "HIGH" }],
      evidences: [
        { id: "e1", kind: "REPOSITORY", title: "rota-dev", detail: "Java 70% · CI com GitHub Actions", url: "https://github.com/x/rota-dev", confidence: "HIGH" },
      ],
    },
  ],
};
const sources = [{ id: "g1", type: "GITHUB", externalLogin: "octocat", connectedAt: "2026-11-10T11:00:00Z", lastSyncedAt: "2026-11-10T12:00:00Z" }];

function mockApi(overrides: Record<string, () => Response> = {}) {
  const routes: Record<string, () => Response> = {
    "GET /api/sources": () => Response.json(sources),
    "GET /api/sources/uploads": () => Response.json([]),
    "GET /api/profile": () => Response.json(profile),
    ...overrides,
  };
  const fetchMock = vi.fn(async (path: string, init?: RequestInit) => {
    const handler = routes[`${init?.method ?? "GET"} ${path}`];
    return handler ? handler() : new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("ProfileReview", () => {
  it("mostra a fonte GitHub, as habilidades e as evidências", async () => {
    mockApi();

    render(<ProfileReview />);

    expect(await screen.findByText("octocat")).toBeInTheDocument();
    expect(screen.getByText("Java")).toBeInTheDocument();
    expect(screen.getByText("comprovado")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Abrir rota-dev" })).toHaveAttribute("href", "https://github.com/x/rota-dev");
  });

  it("pede para entrar quando não há sessão", async () => {
    mockApi({
      "GET /api/sources": () => new Response(null, { status: 401 }),
      "GET /api/sources/uploads": () => new Response(null, { status: 401 }),
      "GET /api/profile": () => new Response(null, { status: 401 }),
    });

    render(<ProfileReview />);

    expect(await screen.findByRole("link", { name: "Entre com GitHub" })).toBeInTheDocument();
  });

  it("envia o currículo com o token CSRF e mostra o erro da API", async () => {
    const fetchMock = mockApi({
      "POST /api/sources/uploads": () => Response.json({ detail: "Arquivo maior que 10 MB." }, { status: 400 }),
    });
    document.cookie = "XSRF-TOKEN=tok";
    render(<ProfileReview />);
    await screen.findByText("octocat");

    const input = screen.getByLabelText("Arquivo") as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(["%PDF-1.7"], "cv.pdf", { type: "application/pdf" })] } });
    fireEvent.submit(input.form!);

    expect(await screen.findByRole("alert")).toHaveTextContent("Arquivo maior que 10 MB.");
    const call = fetchMock.mock.calls.find(([path, init]) => path === "/api/sources/uploads" && init?.method === "POST");
    expect(new Headers(call?.[1]?.headers).get("X-XSRF-TOKEN")).toBe("tok");
    expect((call?.[1]?.body as FormData).get("kind")).toBe("RESUME");
    await waitFor(() => expect(screen.getByRole("button", { name: "Enviar" })).toBeEnabled());
  });
});
