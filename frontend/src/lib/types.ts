// Mirrors the Spring Boot entities/DTOs so mock and API data share one contract.

export const SENTIMENTS = ["POSITIVE", "NEUTRAL", "NEGATIVE"] as const;
export type Sentiment = (typeof SENTIMENTS)[number];

export const TOPICS = [
  "CLEANLINESS",
  "STAFF",
  "ACCESSIBILITY",
  "FOOD",
  "LOCATION",
  "AMENITIES",
  "VALUE",
  "COMFORT",
  "CHECK_IN",
  "CHECK_OUT",
  "NOISE",
  "SAFETY",
  "OTHER",
] as const;
export type Topic = (typeof TOPICS)[number];

export const ANALYSIS_STATUSES = ["PENDING", "PROCESSING", "COMPLETED", "FAILED"] as const;
export type AnalysisStatus = (typeof ANALYSIS_STATUSES)[number];

export const PAGE_SIZES = [10, 20, 50] as const;

export type AnalysisSource = "AI" | "FALLBACK";

export interface ReviewAnalysis {
  sentiment: Sentiment;
  sentimentScore: number;
  topics: Topic[];
  mainTopic: Topic;
  managerResponse: string;
  source: AnalysisSource | null;
  modelName: string | null;
  promptVersion: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: number;
  guestName: string;
  reviewText: string;
  rating: number | null;
  analysisStatus: AnalysisStatus;
  analysisError: string | null;
  analysisUpdatedAt: string | null;
  submittedAt: string;
  updatedAt: string;
  analysis: ReviewAnalysis | null;
}

export interface ReviewListItem {
  id: number;
  guestName: string;
  rating: number | null;
  analysisStatus: AnalysisStatus;
  sentiment: Sentiment | null;
  mainTopic: Topic | null;
  submittedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReviewFilters {
  status?: AnalysisStatus;
  sentiment?: Sentiment;
  topic?: Topic;
  ratingMin?: number;
  ratingMax?: number;
  /** yyyy-MM-dd */
  dateFrom?: string;
  /** yyyy-MM-dd */
  dateTo?: string;
  guest?: string;
}

export interface ReviewQuery extends ReviewFilters {
  page: number;
  size: number;
}

export interface ReviewDetail {
  review: Review;
  policyContext: string;
  ragEnabled: boolean;
}

export interface ReviewSubmission {
  guestName: string;
  reviewText: string;
  rating: number | null;
}

export interface DashboardMetrics {
  totalReviews: number;
  averageRating: number;
  mostCommonTopic: string;
  mostCommonRating: string;
  sentimentCounts: Record<Sentiment, number>;
  topicCounts: Partial<Record<Topic, number>>;
  ratingCounts: Record<"1" | "2" | "3" | "4" | "5", number>;
}

export interface AiStatus {
  /** "ollama" or "openai" (any OpenAI-compatible API). */
  provider: string;
  baseUrl: string;
  model: string;
  reachable: boolean;
  modelAvailable: boolean;
  message: string;
}

export interface CurrentUser {
  username: string;
  roles: string[];
}

export interface Policy {
  id: number;
  title: string;
  category: string;
  content: string;
  tags: string[];
  source: string | null;
  /** yyyy-MM-dd */
  effectiveDate: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type PolicyInput = Pick<Policy, "title" | "category" | "content" | "tags" | "source" | "effectiveDate" | "active">;

export interface ReindexResult {
  ragEnabled: boolean;
  indexed: number;
}
