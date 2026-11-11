"use client";

import useSWR, { type SWRConfiguration } from "swr";

import {
  getCheckinStatus,
  getCurrentPlan,
  getGoal,
  getLatestDiagnosis,
  getLatestPlan,
  getMe,
  getNow,
  getPlanVersion,
  getProfile,
  getReadiness,
  listAdjustments,
  listMilestones,
  listPlanVersions,
  listSources,
  listUploads,
} from "./client";

/**
 * Camada de dados das telas (ADR a registrar: SWR). Cada hook é uma chave de cache + a função de
 * `client.ts`. Em Python seria um cache de requests com `refetch` automático: duas telas que pedem o
 * mesmo dado compartilham uma só chamada, e `mutate(chave)` refaz a busca depois de uma escrita.
 * Os hooks não conhecem HTTP: isso continua só em `client.ts`.
 */
const defaults: SWRConfiguration = { revalidateOnFocus: false, shouldRetryOnError: false };

export const keys = {
  me: "/api/me",
  now: "/api/now",
  readiness: "/api/readiness",
  goal: "/api/goal",
  diagnosis: "/api/diagnosis/latest",
  planLatest: "/api/plan/latest",
  planCurrent: "/api/plan/current",
  planVersions: "/api/plan/versions",
  checkin: "/api/checkin",
  milestones: "/api/milestones",
  adjustments: "/api/adjustments",
  sources: "/api/sources",
  uploads: "/api/sources/uploads",
  profile: "/api/profile",
} as const;

/** `null` quando não há sessão (401). */
export const useMe = () => useSWR(keys.me, getMe, defaults);
export const useNow = () => useSWR(keys.now, getNow, defaults);
export const useReadiness = () => useSWR(keys.readiness, getReadiness, defaults);
export const useGoal = () => useSWR(keys.goal, getGoal, defaults);
export const useCheckinStatus = () => useSWR(keys.checkin, getCheckinStatus, defaults);
export const useMilestones = () => useSWR(keys.milestones, listMilestones, defaults);
export const useAdjustments = () => useSWR(keys.adjustments, listAdjustments, defaults);
export const useSources = () => useSWR(keys.sources, listSources, defaults);
export const useUploads = () => useSWR(keys.uploads, listUploads, defaults);
export const useProfile = () => useSWR(keys.profile, getProfile, defaults);
export const useCurrentPlan = () => useSWR(keys.planCurrent, getCurrentPlan, defaults);
export const usePlanVersions = () => useSWR(keys.planVersions, listPlanVersions, defaults);

/** Conteúdo completo de uma versão do plano; `id` nulo não busca nada (usado ao comparar versões). */
export const usePlanVersion = (id: string | null) => useSWR(id ? `/api/plan/versions/${id}` : null, () => getPlanVersion(id as string), defaults);

/** Diagnóstico e plano são gerados num job: enquanto estão `PENDING`, refaz a busca a cada 3 s. */
export function useLatestDiagnosis() {
  return useSWR(keys.diagnosis, getLatestDiagnosis, {
    ...defaults,
    refreshInterval: (latest) => (latest?.status === "PENDING" ? 3000 : 0),
  });
}

export function useLatestPlan() {
  return useSWR(keys.planLatest, getLatestPlan, {
    ...defaults,
    refreshInterval: (latest) => (latest?.status === "PENDING" ? 3000 : 0),
  });
}

export type Job = { id: string; label: string };

/** Trabalhos assíncronos em andamento, derivados do status do diagnóstico e do plano (a API não tem endpoint de jobs). */
export function useJobs(): Job[] {
  const { data: diagnosis } = useLatestDiagnosis();
  const { data: plan } = useLatestPlan();
  const jobs: Job[] = [];
  if (diagnosis?.status === "PENDING") jobs.push({ id: "diagnosis", label: "Gerando diagnóstico" });
  if (plan?.status === "PENDING") jobs.push({ id: "plan", label: "Gerando plano" });
  return jobs;
}
