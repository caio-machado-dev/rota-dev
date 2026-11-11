import type { Metadata } from "next";

import { AdjustmentsSection } from "@/components/progress/adjustments-section";

export const metadata: Metadata = { title: "Reajustes" };

export default function Page() {
  return <AdjustmentsSection />;
}
