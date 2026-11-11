import type { Metadata } from "next";

import { GoalSection } from "@/components/plan/goal-section";

export const metadata: Metadata = { title: "Objetivo" };

export default function GoalPage() {
  return <GoalSection />;
}
