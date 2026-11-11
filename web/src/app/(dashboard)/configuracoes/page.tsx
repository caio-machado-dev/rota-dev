import type { Metadata } from "next";

import { SettingsPanel } from "@/components/settings/settings-panel";
import { PageHeader } from "@/components/system/page-header";

export const metadata: Metadata = { title: "Configurações" };

export default function SettingsPage() {
  return (
    <>
      <PageHeader title="Configurações" description="Aparência e conta." />
      <SettingsPanel />
    </>
  );
}
