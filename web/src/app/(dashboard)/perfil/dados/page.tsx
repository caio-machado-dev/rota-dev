import type { Metadata } from "next";

import { AccountSection } from "@/components/profile/account-section";

export const metadata: Metadata = { title: "Meus dados" };

export default function AccountPage() {
  return <AccountSection />;
}
