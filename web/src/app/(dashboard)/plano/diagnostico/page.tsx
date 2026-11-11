import type { Metadata } from "next";

import { DiagnosisSection } from "@/components/plan/diagnosis-section";

export const metadata: Metadata = { title: "Diagnóstico" };

export default function DiagnosisPage() {
  return <DiagnosisSection />;
}
