import type {
  AiStatus,
  CurrentUser,
  DashboardMetrics,
  Page,
  Review,
  ReviewDetail,
  ReviewListItem,
  ReviewQuery,
  ReviewSubmission,
} from "@/lib/types";

export type DataSourceMode = "mock" | "api";

export interface DataSource {
  mode: DataSourceMode;
  getDashboard(): Promise<DashboardMetrics>;
  listReviews(query: ReviewQuery): Promise<Page<ReviewListItem>>;
  getReview(id: number): Promise<ReviewDetail>;
  submitReview(submission: ReviewSubmission): Promise<Review>;
  retryAnalysis(id: number): Promise<Review>;
  getAiStatus(): Promise<AiStatus>;
  getCurrentUser(): Promise<CurrentUser | null>;
  login(username: string, password: string): Promise<CurrentUser>;
  logout(): Promise<void>;
  resetDemo?(): Promise<void>;
}
