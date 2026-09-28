import { Loader2Icon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { humanize } from "@/lib/format";
import type { AnalysisStatus, Sentiment } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<AnalysisStatus, string> = {
  PENDING: "bg-muted text-muted-foreground",
  PROCESSING: "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300",
  COMPLETED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
  FAILED: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
};

const SENTIMENT_STYLES: Record<Sentiment, string> = {
  POSITIVE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
  NEUTRAL: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
  NEGATIVE: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
};

export function StatusBadge({ status }: { status: AnalysisStatus }) {
  return (
    <Badge className={cn(STATUS_STYLES[status])}>
      {status === "PROCESSING" && <Loader2Icon className="animate-spin" />}
      {humanize(status)}
    </Badge>
  );
}

export function SentimentBadge({ sentiment }: { sentiment: Sentiment | null }) {
  if (!sentiment) return <span className="text-muted-foreground">—</span>;
  return <Badge className={cn(SENTIMENT_STYLES[sentiment])}>{humanize(sentiment)}</Badge>;
}

export function RatingStars({ rating }: { rating: number | null }) {
  if (rating == null) return <span className="text-muted-foreground">N/A</span>;
  return (
    <span className="whitespace-nowrap text-amber-500" aria-label={`${rating} out of 5`}>
      {"★".repeat(rating)}
      <span className="text-muted-foreground/40">{"★".repeat(5 - rating)}</span>
    </span>
  );
}
