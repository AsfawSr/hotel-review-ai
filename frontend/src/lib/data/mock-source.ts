import { analyzeReview } from "@/lib/mock/analysis";
import { buildPolicyContext, buildSeedPolicies } from "@/lib/mock/policies";
import { buildSeedReviews } from "@/lib/mock/seed";
import type {
  AppUser,
  DashboardMetrics,
  Page,
  Policy,
  PolicyInput,
  Reply,
  ReplyRevision,
  ReplyStatus,
  Review,
  ReviewListItem,
  ReviewQuery,
  Sentiment,
  Topic,
} from "@/lib/types";
import { SENTIMENTS } from "@/lib/types";
import { validatePolicy, validateSubmission, validateUser } from "@/lib/validation";
import type { DataSource } from "./data-source";
import { ApiError } from "./errors";

const STORAGE_KEY = "hotel-review-ai:demo:v1";
const SESSION_KEY = "hotel-review-ai:demo:session";
const LATENCY_MS = 250;
const PENDING_MS = 1_500;
const PROCESSING_MS = 3_500;

export const DEMO_CREDENTIALS = { username: "demo", password: "demo123" } as const;

interface State {
  reviews: Review[];
  policies: Policy[];
  users: AppUser[];
  /** Reply revisions per review id (oldest first). */
  replies: Record<number, ReplyRevision[]>;
  /** When the timeline was last aligned to "now" (epoch ms). */
  anchoredAt: number;
}

const delay = <T>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), LATENCY_MS));
const hasStorage = () => typeof window !== "undefined" && !!window.localStorage;
const seedUsers = (): AppUser[] => {
  const now = new Date().toISOString();
  return [
    { id: 1, username: DEMO_CREDENTIALS.username, role: "ADMIN", enabled: true, createdAt: now },
    { id: 2, username: "frontdesk", role: "MANAGER", enabled: true, createdAt: now },
    { id: 3, username: "auditor", role: "VIEWER", enabled: true, createdAt: now },
  ];
};
const seedState = (): State => ({
  reviews: buildSeedReviews(),
  policies: buildSeedPolicies(),
  users: seedUsers(),
  replies: {},
  anchoredAt: Date.now(),
});
const REANCHOR_AFTER_MS = 24 * 60 * 60 * 1000;

const shiftIso = (iso: string, ms: number) => new Date(Date.parse(iso) + ms).toISOString();
const shiftNullable = (iso: string | null, ms: number) => (iso ? shiftIso(iso, ms) : iso);

// Moves every timestamp forward by the time since the last visit so the demo never looks stale.
export function reanchor(state: State, now: number): State {
  const delta = now - state.anchoredAt;
  if (delta < REANCHOR_AFTER_MS) return state;
  return {
    ...state,
    anchoredAt: now,
    reviews: state.reviews.map((r) => ({
      ...r,
      submittedAt: shiftIso(r.submittedAt, delta),
      updatedAt: shiftIso(r.updatedAt, delta),
      analysisUpdatedAt: shiftNullable(r.analysisUpdatedAt, delta),
      analysis: r.analysis && {
        ...r.analysis,
        createdAt: shiftIso(r.analysis.createdAt, delta),
        updatedAt: shiftIso(r.analysis.updatedAt, delta),
      },
    })),
  };
}

function readState(): State {
  if (hasStorage()) {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const stored = JSON.parse(raw) as Partial<State>;
        // Older demo data predates policies and anchoring.
        const state: State = {
          reviews: stored.reviews ?? buildSeedReviews(),
          policies: stored.policies ?? buildSeedPolicies(),
          users: stored.users ?? seedUsers(),
          replies: stored.replies ?? {},
          anchoredAt: stored.anchoredAt ?? Date.now(),
        };
        return reanchor(state, Date.now());
      }
    } catch {
      // Corrupt storage falls through to a fresh seed.
    }
  }
  return seedState();
}

function writeState(state: State) {
  if (hasStorage()) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

// Derive simulated async progress from timestamps so it survives page reloads.
function advance(review: Review, now: number): Review {
  let current = review;
  if (current.analysisStatus === "PENDING") {
    const started = Date.parse(current.analysisUpdatedAt ?? current.submittedAt) + PENDING_MS;
    if (now < started) return current;
    const ts = new Date(started).toISOString();
    current = { ...current, analysisStatus: "PROCESSING", analysisUpdatedAt: ts, updatedAt: ts };
  }
  if (current.analysisStatus === "PROCESSING") {
    const finished = Date.parse(current.analysisUpdatedAt ?? current.submittedAt) + PROCESSING_MS;
    if (now < finished) return current;
    const ts = new Date(finished).toISOString();
    current = {
      ...current,
      analysisStatus: "COMPLETED",
      analysisError: null,
      analysisUpdatedAt: ts,
      updatedAt: ts,
      analysis: analyzeReview(current.guestName, current.reviewText, current.rating, ts),
    };
  }
  return current;
}

function withState<T>(fn: (state: State) => T): T {
  const state = readState();
  const now = Date.now();
  state.reviews = state.reviews.map((r) => advance(r, now));
  const result = fn(state);
  writeState(state);
  return result;
}

function findReview(state: State, id: number): Review {
  const review = state.reviews.find((r) => r.id === id);
  if (!review) throw new ApiError("Review not found", 404);
  return review;
}

function assertValidPolicy(input: PolicyInput) {
  const fieldErrors = validatePolicy(input);
  if (Object.keys(fieldErrors).length) throw new ApiError("Validation failed", 400, fieldErrors);
}

function currentReply(state: State, reviewId: number): Reply {
  const review = findReview(state, reviewId);
  if (!review.analysis) throw new ApiError("The review has not been analyzed yet", 409);
  const history = state.replies[reviewId] ?? [];
  const last = history.at(-1);
  return last
    ? { status: last.status, text: last.text, updatedBy: last.author, updatedAt: last.createdAt, history }
    : { status: "DRAFT", text: review.analysis.managerResponse, updatedBy: "AI", updatedAt: null, history: [] };
}

// Same rules as ReplyStatus on the backend.
function canTransition(from: ReplyStatus, to: ReplyStatus): boolean {
  if (to === "EDITED") return from !== "SENT";
  if (to === "APPROVED") return from === "DRAFT" || from === "EDITED";
  if (to === "SENT") return from === "APPROVED";
  return false;
}

async function transitionReply(reviewId: number, to: ReplyStatus, text?: string): Promise<Reply> {
  const reply = withState((s) => {
    const current = currentReply(s, reviewId);
    if (!canTransition(current.status, to)) throw new ApiError(`Cannot change a ${current.status} reply to ${to}`, 409);
    const author = hasStorage() ? (window.localStorage.getItem(SESSION_KEY) ?? "demo") : "demo";
    const revision: ReplyRevision = { status: to, text: text ?? current.text, author, createdAt: new Date().toISOString() };
    s.replies[reviewId] = [...(s.replies[reviewId] ?? []), revision];
    return currentReply(s, reviewId);
  });
  return delay(reply);
}

const normalizePolicy = (input: PolicyInput): PolicyInput => ({
  title: input.title.trim(),
  category: input.category.trim(),
  content: input.content.trim(),
  tags: [...new Set(input.tags.map((t) => t.trim()))],
  source: input.source?.trim() || null,
  effectiveDate: input.effectiveDate || null,
  active: input.active,
});

const toListItem = (r: Review): ReviewListItem => ({
  id: r.id,
  guestName: r.guestName,
  rating: r.rating,
  analysisStatus: r.analysisStatus,
  sentiment: r.analysis?.sentiment ?? null,
  mainTopic: r.analysis?.mainTopic ?? null,
  submittedAt: r.submittedAt,
});

function filterReviews(reviews: Review[], q: ReviewQuery): Review[] {
  let min = q.ratingMin == null ? undefined : Math.max(1, Math.min(5, q.ratingMin));
  let max = q.ratingMax == null ? undefined : Math.max(1, Math.min(5, q.ratingMax));
  if (min != null && max != null && min > max) [min, max] = [max, min];
  const from = q.dateFrom ? Date.parse(`${q.dateFrom}T00:00:00.000Z`) : undefined;
  const to = q.dateTo ? Date.parse(`${q.dateTo}T23:59:59.999Z`) : undefined;
  const guest = q.guest?.trim().toLowerCase();

  return reviews.filter((r) => {
    const submitted = Date.parse(r.submittedAt);
    if (q.status && r.analysisStatus !== q.status) return false;
    if (q.sentiment && r.analysis?.sentiment !== q.sentiment) return false;
    if (q.topic && r.analysis?.mainTopic !== q.topic) return false;
    if (min != null && (r.rating == null || r.rating < min)) return false;
    if (max != null && (r.rating == null || r.rating > max)) return false;
    if (from != null && submitted < from) return false;
    if (to != null && submitted > to) return false;
    if (guest && !r.guestName.toLowerCase().includes(guest)) return false;
    return true;
  });
}

function topEntry(counts: Record<string, number>): string {
  const [top] = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  return top && top[1] > 0 ? top[0] : "N/A";
}

function buildDashboard(reviews: Review[]): DashboardMetrics {
  const rated = reviews.filter((r) => r.rating != null);
  const sentimentCounts = Object.fromEntries(SENTIMENTS.map((s) => [s, 0])) as Record<Sentiment, number>;
  const topicCounts: Partial<Record<Topic, number>> = {};
  const ratingCounts = { "1": 0, "2": 0, "3": 0, "4": 0, "5": 0 };

  for (const r of reviews) {
    if (r.analysis) {
      sentimentCounts[r.analysis.sentiment]++;
      topicCounts[r.analysis.mainTopic] = (topicCounts[r.analysis.mainTopic] ?? 0) + 1;
    }
    if (r.rating != null) ratingCounts[String(r.rating) as keyof typeof ratingCounts]++;
  }

  const sortedTopics = Object.fromEntries(
    Object.entries(topicCounts).sort((a, b) => b[1] - a[1]),
  ) as Partial<Record<Topic, number>>;

  return {
    totalReviews: reviews.length,
    averageRating: rated.length ? rated.reduce((sum, r) => sum + (r.rating ?? 0), 0) / rated.length : 0,
    mostCommonTopic: topEntry(sortedTopics as Record<string, number>),
    mostCommonRating: topEntry(ratingCounts),
    sentimentCounts,
    topicCounts: sortedTopics,
    ratingCounts,
  };
}

export const mockDataSource: DataSource = {
  mode: "mock",

  getDashboard: () => delay(withState((s) => buildDashboard(s.reviews))),

  listReviews: (query) =>
    delay(
      withState((s): Page<ReviewListItem> => {
        const size = Math.min(Math.max(1, query.size), 100);
        const filtered = filterReviews(s.reviews, query).sort((a, b) => b.submittedAt.localeCompare(a.submittedAt));
        const totalPages = Math.max(1, Math.ceil(filtered.length / size));
        const page = Math.min(Math.max(0, query.page), totalPages - 1);
        return {
          content: filtered.slice(page * size, page * size + size).map(toListItem),
          page,
          size,
          totalElements: filtered.length,
          totalPages,
        };
      }),
    ),

  getReview: async (id) => {
    const { review, policies } = withState((s) => ({ review: findReview(s, id), policies: s.policies }));
    // Mirrors the backend: only AI analyses are grounded in retrieved policies.
    if (!review.analysis || review.analysis.source === "FALLBACK") {
      return delay({ review, policyContext: "", ragEnabled: false });
    }
    return delay({ review, policyContext: buildPolicyContext(policies, review.analysis.topics), ragEnabled: true });
  },

  submitReview: async (submission) => {
    const fieldErrors = validateSubmission(submission);
    if (Object.keys(fieldErrors).length) throw new ApiError("Validation failed", 400, fieldErrors);

    const review = withState((s) => {
      const now = new Date().toISOString();
      const created: Review = {
        id: s.reviews.reduce((max, r) => Math.max(max, r.id), 0) + 1,
        guestName: submission.guestName.trim(),
        reviewText: submission.reviewText.trim(),
        rating: submission.rating,
        analysisStatus: "PENDING",
        analysisError: null,
        analysisUpdatedAt: now,
        submittedAt: now,
        updatedAt: now,
        analysis: null,
      };
      s.reviews.push(created);
      return created;
    });
    return delay(review);
  },

  retryAnalysis: async (id) => {
    const review = withState((s) => {
      const index = s.reviews.findIndex((r) => r.id === id);
      const existing = findReview(s, id);
      if (existing.analysisStatus === "PROCESSING") return existing;
      const now = new Date().toISOString();
      const queued: Review = { ...existing, analysisStatus: "PENDING", analysisError: null, analysisUpdatedAt: now, updatedAt: now };
      s.reviews[index] = queued;
      return queued;
    });
    return delay(review);
  },

  getReply: async (reviewId) => delay(withState((s) => currentReply(s, reviewId))),

  editReply: async (reviewId, text) => {
    const trimmed = text.trim();
    if (!trimmed || trimmed.length > 4000) throw new ApiError("Validation failed", 400, { text: "Reply must be 1-4000 characters." });
    return transitionReply(reviewId, "EDITED", trimmed);
  },

  approveReply: (reviewId) => transitionReply(reviewId, "APPROVED"),

  markReplySent: (reviewId) => transitionReply(reviewId, "SENT"),

  getAiStatus: () =>
    delay({
      provider: "ollama",
      baseUrl: "in-browser (demo)",
      model: "llama3.2:latest (simulated)",
      reachable: true,
      modelAvailable: true,
      message: "Demo mode: analysis is simulated in your browser using the backend's fallback heuristics.",
    }),

  listPolicies: () =>
    delay(withState((s) => [...s.policies].sort((a, b) => a.category.localeCompare(b.category) || a.title.localeCompare(b.title)))),

  createPolicy: async (input) => {
    assertValidPolicy(input);
    const created = withState((s) => {
      const now = new Date().toISOString();
      const policy: Policy = { ...normalizePolicy(input), id: s.policies.reduce((max, p) => Math.max(max, p.id), 0) + 1, createdAt: now, updatedAt: now };
      s.policies.push(policy);
      return policy;
    });
    return delay(created);
  },

  updatePolicy: async (id, input) => {
    assertValidPolicy(input);
    const updated = withState((s) => {
      const index = s.policies.findIndex((p) => p.id === id);
      if (index < 0) throw new ApiError("Policy not found", 404);
      s.policies[index] = { ...s.policies[index], ...normalizePolicy(input), updatedAt: new Date().toISOString() };
      return s.policies[index];
    });
    return delay(updated);
  },

  deletePolicy: async (id) => {
    withState((s) => {
      if (!s.policies.some((p) => p.id === id)) throw new ApiError("Policy not found", 404);
      s.policies = s.policies.filter((p) => p.id !== id);
    });
    return delay(undefined);
  },

  reindexPolicies: () => delay(withState((s) => ({ ragEnabled: true, indexed: s.policies.filter((p) => p.active).length }))),

  listUsers: () => delay(withState((s) => [...s.users].sort((a, b) => a.username.localeCompare(b.username)))),

  createUser: async (input) => {
    const fieldErrors = validateUser(input, true);
    if (Object.keys(fieldErrors).length) throw new ApiError("Validation failed", 400, fieldErrors);
    const created = withState((s) => {
      const username = input.username.trim();
      if (s.users.some((u) => u.username.toLowerCase() === username.toLowerCase())) {
        throw new ApiError("Username already exists", 409);
      }
      // Demo mode never stores passwords; only the demo account can sign in.
      const user: AppUser = { id: s.users.reduce((max, u) => Math.max(max, u.id), 0) + 1, username, role: input.role, enabled: true, createdAt: new Date().toISOString() };
      s.users.push(user);
      return user;
    });
    return delay(created);
  },

  updateUser: async (id, input) => {
    const fieldErrors = validateUser({ password: input.password }, false);
    if (Object.keys(fieldErrors).length) throw new ApiError("Validation failed", 400, fieldErrors);
    const updated = withState((s) => {
      const index = s.users.findIndex((u) => u.id === id);
      if (index < 0) throw new ApiError("User not found", 404);
      const current = s.users[index];
      const activeAdmins = s.users.filter((u) => u.role === "ADMIN" && u.enabled).length;
      const losesAdmin = current.role === "ADMIN" && current.enabled && (input.role !== "ADMIN" || !input.enabled);
      if (losesAdmin && activeAdmins <= 1) throw new ApiError("Cannot demote or disable the last active admin", 409);
      s.users[index] = { ...current, role: input.role, enabled: input.enabled };
      return s.users[index];
    });
    return delay(updated);
  },

  getCurrentUser: async () => {
    const username = hasStorage() ? window.localStorage.getItem(SESSION_KEY) : null;
    return delay(username ? { username, roles: ["ADMIN"] } : null);
  },

  login: async (username, password) => {
    if (username !== DEMO_CREDENTIALS.username || password !== DEMO_CREDENTIALS.password) {
      await delay(null);
      throw new ApiError("Invalid username or password.", 401);
    }
    if (hasStorage()) window.localStorage.setItem(SESSION_KEY, username);
    return delay({ username, roles: ["ADMIN"] });
  },

  logout: async () => {
    if (hasStorage()) window.localStorage.removeItem(SESSION_KEY);
    return delay(undefined);
  },

  resetDemo: async () => {
    writeState(seedState());
    return delay(undefined);
  },
};
