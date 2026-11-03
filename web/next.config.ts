import type { NextConfig } from "next";

/**
 * Endereço da API visto pelo servidor do Next.js.
 * Local: http://localhost:8080. No Docker Compose: http://api:8080 (nome do serviço na rede interna).
 * Atenção: os rewrites são resolvidos no `next build`, então no Docker o valor entra como build arg.
 */
const apiUrl = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Gera um servidor Node mínimo em .next/standalone, para uma imagem Docker enxuta (ADR 0013).
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // O navegador sempre chama /api/... na mesma origem do front; o Next repassa para a API.
  // Assim o cookie de sessão funciona sem CORS (ADR 0005).
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }];
  },
};

export default nextConfig;
