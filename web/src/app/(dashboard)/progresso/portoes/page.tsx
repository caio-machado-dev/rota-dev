import type { Metadata } from "next";

import { GatesSection } from "@/components/progress/gates-section";

export const metadata: Metadata = { title: "Portões" };

export default function Page() {
  return <GatesSection />;
}
