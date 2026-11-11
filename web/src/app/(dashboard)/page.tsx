import type { Metadata } from "next";

import { NowDashboard } from "@/components/now/now-dashboard";

export const metadata: Metadata = { title: "Agora" };

export default function NowPage() {
  return <NowDashboard />;
}
