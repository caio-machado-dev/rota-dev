import type { Metadata } from "next";

import { AccountData } from "@/components/profile/account-data";
import { ProfileReview } from "@/components/profile/profile-review";

export const metadata: Metadata = { title: "Perfil" };

export default function ProfilePage() {
  return (
    <section className="max-w-3xl">
      <h1 className="text-2xl font-semibold tracking-tight">Perfil</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        O que as suas fontes mostram, sempre com a evidência ao lado. É a base do diagnóstico.
      </p>
      <div className="mt-8">
        <ProfileReview />
      </div>
      <div className="mt-16 border-t border-border pt-8">
        <AccountData />
      </div>
    </section>
  );
}
