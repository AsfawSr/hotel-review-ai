import type {
  AiStatus,
  AppUser,
  CurrentUser,
  DashboardMetrics,
  Page,
  Policy,
  PolicyInput,
  ReindexResult,
  Reply,
  Review,
  ReviewDetail,
  ReviewListItem,
  ReviewQuery,
  ReviewSubmission,
  UserCreateInput,
  UserUpdateInput,
} from "@/lib/types";

export type DataSourceMode = "mock" | "api";

export interface DataSource {
  mode: DataSourceMode;
  getDashboard(): Promise<DashboardMetrics>;
  listReviews(query: ReviewQuery): Promise<Page<ReviewListItem>>;
  getReview(id: number): Promise<ReviewDetail>;
  submitReview(submission: ReviewSubmission): Promise<Review>;
  retryAnalysis(id: number): Promise<Review>;
  getReply(reviewId: number): Promise<Reply>;
  editReply(reviewId: number, text: string): Promise<Reply>;
  approveReply(reviewId: number): Promise<Reply>;
  markReplySent(reviewId: number): Promise<Reply>;
  getAiStatus(): Promise<AiStatus>;
  listPolicies(): Promise<Policy[]>;
  createPolicy(input: PolicyInput): Promise<Policy>;
  updatePolicy(id: number, input: PolicyInput): Promise<Policy>;
  deletePolicy(id: number): Promise<void>;
  reindexPolicies(): Promise<ReindexResult>;
  listUsers(): Promise<AppUser[]>;
  createUser(input: UserCreateInput): Promise<AppUser>;
  updateUser(id: number, input: UserUpdateInput): Promise<AppUser>;
  getCurrentUser(): Promise<CurrentUser | null>;
  login(username: string, password: string): Promise<CurrentUser>;
  logout(): Promise<void>;
  resetDemo?(): Promise<void>;
}
