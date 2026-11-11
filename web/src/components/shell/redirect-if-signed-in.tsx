"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useMe } from "@/lib/api/hooks";

/** Quem já tem sessão e abre `/login` vai direto para Agora. */
export function RedirectIfSignedIn() {
  const { data: me } = useMe();
  const router = useRouter();
  useEffect(() => {
    if (me) router.replace("/");
  }, [me, router]);
  return null;
}
