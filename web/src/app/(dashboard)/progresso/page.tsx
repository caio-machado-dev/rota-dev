import type { Metadata } from "next";

import { ProgressOverview } from "@/components/progress/progress-overview";

export const metadata: Metadata = { title: "Progresso" };

export default function ProgressPage() {
  return <ProgressOverview />;
}
