import type { Metadata } from "next";

import { RoadmapSection } from "@/components/plan/roadmap-section";

export const metadata: Metadata = { title: "Roadmap" };

export default function RoadmapPage() {
  return <RoadmapSection />;
}
