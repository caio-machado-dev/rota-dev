import type { Metadata } from "next";

import { SourcesSection } from "@/components/profile/sources-section";

export const metadata: Metadata = { title: "Fontes" };

export default function SourcesPage() {
  return <SourcesSection />;
}
