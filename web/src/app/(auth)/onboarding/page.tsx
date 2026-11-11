import type { Metadata } from "next";
import Link from "next/link";

import { Logo } from "@/components/brand/logo";
import { OnboardingWizard } from "@/components/onboarding/onboarding-wizard";

export const metadata: Metadata = { title: "Primeiros passos" };

export default function OnboardingPage() {
  return (
    <main className="min-h-dvh px-4 py-8 md:py-12">
      <div className="mx-auto mb-10 flex max-w-3xl items-center justify-between">
        <Link href="/" aria-label="Ir para Agora">
          <Logo />
        </Link>
      </div>
      <OnboardingWizard />
    </main>
  );
}
