import type {
  AiStatus,
  AppUser,
  CurrentUser,
  DashboardMetrics,
  Page,
  Policy,
  ReindexResult,
  Reply,
  Review,
  ReviewDetail,
  ReviewListItem,
  ReviewQuery,
  WeeklyTrend,
} from "@/lib/types";
import type { DataSource } from "./data-source";
import { ApiError } from "./errors";

// Same-origin path; next.config.ts rewrites it to BACKEND_URL so session cookies stay first-party.
const API_BASE = "/api/v1";

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  const match = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.split("=")[1]) : undefined;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  // Spring Security's CookieCsrfTokenRepository convention.
  const csrf = readCookie("XSRF-TOKEN");
  if (csrf && init.method && init.method !== "GET") headers.set("X-XSRF-TOKEN", csrf);

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: "include" });

  if (!response.ok) {
    let message = response.statusText || "Request failed";
    let fieldErrors: Record<string, string> = {};
    try {
      const problem = await response.json();
      message = problem.detail ?? problem.title ?? message;
      fieldErrors = problem.errors ?? {};
    } catch {
      // Non-JSON error body.
    }
    throw new ApiError(message, response.status, fieldErrors);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function toSearchParams(query: ReviewQuery): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== "") params.set(key, String(value));
  }
  return params.toString();
}

export const apiDataSource: DataSource = {
  mode: "api",
  getDashboard: () => request<DashboardMetrics>("/dashboard"),
  getTrends: (weeks) => request<WeeklyTrend[]>(`/dashboard/trends?weeks=${weeks}`),
  listReviews: (query) => request<Page<ReviewListItem>>(`/reviews?${toSearchParams(query)}`),
  getReview: (id) => request<ReviewDetail>(`/reviews/${id}`),
  submitReview: (submission) => request<Review>("/reviews", { method: "POST", body: JSON.stringify(submission) }),
  retryAnalysis: (id) => request<Review>(`/reviews/${id}/retry`, { method: "POST" }),
  getReply: (reviewId) => request<Reply>(`/reviews/${reviewId}/reply`),
  editReply: (reviewId, text) => request<Reply>(`/reviews/${reviewId}/reply`, { method: "PUT", body: JSON.stringify({ text }) }),
  approveReply: (reviewId) => request<Reply>(`/reviews/${reviewId}/reply/approve`, { method: "POST" }),
  markReplySent: (reviewId) => request<Reply>(`/reviews/${reviewId}/reply/sent`, { method: "POST" }),
  getAiStatus: () => request<AiStatus>("/ai/status"),
  listPolicies: () => request<Policy[]>("/policies"),
  createPolicy: (input) => request<Policy>("/policies", { method: "POST", body: JSON.stringify(input) }),
  updatePolicy: (id, input) => request<Policy>(`/policies/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  deletePolicy: (id) => request<void>(`/policies/${id}`, { method: "DELETE" }),
  reindexPolicies: () => request<ReindexResult>("/policies/reindex", { method: "POST" }),
  listUsers: () => request<AppUser[]>("/users"),
  createUser: (input) => request<AppUser>("/users", { method: "POST", body: JSON.stringify(input) }),
  updateUser: (id, input) => request<AppUser>(`/users/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  getCurrentUser: async () => {
    try {
      return await request<CurrentUser>("/auth/me");
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null;
      throw error;
    }
  },
  login: (username, password) =>
    request<CurrentUser>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) }),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
};
