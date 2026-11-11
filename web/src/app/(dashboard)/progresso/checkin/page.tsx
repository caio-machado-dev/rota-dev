import type { Metadata } from "next";

import { CheckinSection } from "@/components/progress/checkin-section";

export const metadata: Metadata = { title: "Check-in" };

export default function Page() {
  return <CheckinSection />;
}
