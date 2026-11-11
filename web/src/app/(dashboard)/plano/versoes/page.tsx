import type { Metadata } from "next";

import { VersionsSection } from "@/components/plan/versions-section";

export const metadata: Metadata = { title: "Versões do plano" };

export default function VersionsPage() {
  return <VersionsSection />;
}
