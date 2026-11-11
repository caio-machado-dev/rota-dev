import type { Metadata } from "next";
import { Suspense } from "react";

import { FoundSection } from "@/components/profile/found-section";
import { Skeleton } from "@/components/ui/skeleton";

export const metadata: Metadata = { title: "O que encontramos" };

export default function FoundPage() {
  // `useSearchParams` (filtro vindo da paleta de comandos) exige Suspense no App Router.
  return (
    <Suspense fallback={<Skeleton className="h-96" />}>
      <FoundSection />
    </Suspense>
  );
}
