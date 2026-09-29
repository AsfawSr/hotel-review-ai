"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { PolicyInput, ReviewQuery, ReviewSubmission } from "@/lib/types";
import { dataSource } from "./index";

export const queryKeys = {
  dashboard: ["dashboard"] as const,
  reviews: (query?: ReviewQuery) => (query ? (["reviews", query] as const) : (["reviews"] as const)),
  review: (id: number) => ["review", id] as const,
  aiStatus: ["ai-status"] as const,
  policies: ["policies"] as const,
  currentUser: ["current-user"] as const,
};

const IN_FLIGHT_POLL_MS = 2_000;

export const useDashboard = () => useQuery({ queryKey: queryKeys.dashboard, queryFn: () => dataSource.getDashboard() });

export const useReviews = (query: ReviewQuery) =>
  useQuery({
    queryKey: queryKeys.reviews(query),
    queryFn: () => dataSource.listReviews(query),
    placeholderData: (previous) => previous,
    refetchIntervalInBackground: true,
    refetchInterval: (q) =>
      q.state.data?.content.some((r) => r.analysisStatus === "PENDING" || r.analysisStatus === "PROCESSING")
        ? IN_FLIGHT_POLL_MS
        : false,
  });

export const useReview = (id: number) =>
  useQuery({
    queryKey: queryKeys.review(id),
    queryFn: () => dataSource.getReview(id),
    retry: false,
    refetchIntervalInBackground: true,
    refetchInterval: (q) => {
      const status = q.state.data?.review.analysisStatus;
      return status === "PENDING" || status === "PROCESSING" ? IN_FLIGHT_POLL_MS : false;
    },
  });

export const useAiStatus = () => useQuery({ queryKey: queryKeys.aiStatus, queryFn: () => dataSource.getAiStatus() });

export const useCurrentUser = () =>
  useQuery({ queryKey: queryKeys.currentUser, queryFn: () => dataSource.getCurrentUser(), staleTime: Infinity });

export const useIsAdmin = () => useCurrentUser().data?.roles.includes("ADMIN") ?? false;

export const usePolicies = () => useQuery({ queryKey: queryKeys.policies, queryFn: () => dataSource.listPolicies() });

export function useSavePolicy() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: PolicyInput }) =>
      id == null ? dataSource.createPolicy(input) : dataSource.updatePolicy(id, input),
    onSuccess: () => client.invalidateQueries({ queryKey: queryKeys.policies }),
  });
}

export function useDeletePolicy() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => dataSource.deletePolicy(id),
    onSuccess: () => client.invalidateQueries({ queryKey: queryKeys.policies }),
  });
}

export const useReindexPolicies = () => useMutation({ mutationFn: () => dataSource.reindexPolicies() });

function useInvalidateReviews() {
  const client = useQueryClient();
  return () =>
    Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.reviews() }),
      client.invalidateQueries({ queryKey: queryKeys.dashboard }),
    ]);
}

export function useSubmitReview() {
  const invalidate = useInvalidateReviews();
  return useMutation({
    mutationFn: (submission: ReviewSubmission) => dataSource.submitReview(submission),
    onSuccess: invalidate,
  });
}

export function useRetryAnalysis() {
  const client = useQueryClient();
  const invalidate = useInvalidateReviews();
  return useMutation({
    mutationFn: (id: number) => dataSource.retryAnalysis(id),
    onSuccess: (review) => Promise.all([invalidate(), client.invalidateQueries({ queryKey: queryKeys.review(review.id) })]),
  });
}

export function useLogin() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) => dataSource.login(username, password),
    onSuccess: (user) => client.setQueryData(queryKeys.currentUser, user),
  });
}

export function useLogout() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => dataSource.logout(),
    onSuccess: () => {
      client.setQueryData(queryKeys.currentUser, null);
      client.removeQueries({ predicate: (q) => q.queryKey[0] !== queryKeys.currentUser[0] });
    },
  });
}

export function useResetDemo() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async () => dataSource.resetDemo?.(),
    onSuccess: () => client.invalidateQueries({ predicate: (q) => q.queryKey[0] !== queryKeys.currentUser[0] }),
  });
}
