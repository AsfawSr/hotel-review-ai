"use client";

import { MessageSquareTextIcon, MessageSquareWarningIcon, PlusIcon, StarIcon, TagIcon, TrendingUpIcon } from "lucide-react";
import Link from "next/link";
import { RatingStars, SentimentBadge, StatusBadge } from "@/components/badges";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCanWrite, useDashboard, useReviews, useTrends } from "@/lib/data/hooks";
import { formatDateTime, humanize } from "@/lib/format";
import { RatingChart, SentimentChart, TopicChart, TrendChart } from "./charts";

const TREND_WEEKS = 12;

function WeeklyTrendCard() {
  const { data } = useTrends(TREND_WEEKS);
  return data ? <TrendChart trends={data} /> : <Skeleton className="h-80" />;
}

function MetricCard({ label, value, hint, icon: Icon }: { label: string; value: React.ReactNode; hint?: string; icon: React.ElementType }) {
  return (
    <Card size="sm">
      <CardContent className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
          <p className="mt-1 text-2xl font-semibold">{value}</p>
          {hint && <p className="mt-0.5 text-xs text-muted-foreground">{hint}</p>}
        </div>
        <span className="rounded-lg bg-primary/10 p-2 text-primary">
          <Icon className="size-4" />
        </span>
      </CardContent>
    </Card>
  );
}

function RecentReviews() {
  const { data, isPending } = useReviews({ page: 0, size: 5 });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent reviews</CardTitle>
        <CardDescription>Latest guest feedback</CardDescription>
      </CardHeader>
      <CardContent className="divide-y">
        {isPending
          ? Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="my-2 h-10 w-full" />)
          : data?.content.map((r) => (
              <Link
                key={r.id}
                href={`/reviews/${r.id}`}
                className="flex flex-wrap items-center justify-between gap-2 py-2.5 hover:bg-muted/50 -mx-2 px-2 rounded-md"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{r.guestName}</p>
                  <p className="text-xs text-muted-foreground">{formatDateTime(r.submittedAt)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <RatingStars rating={r.rating} />
                  {r.analysisStatus === "COMPLETED" ? (
                    <SentimentBadge sentiment={r.sentiment} />
                  ) : (
                    <StatusBadge status={r.analysisStatus} />
                  )}
                </div>
              </Link>
            ))}
      </CardContent>
    </Card>
  );
}

export function DashboardView() {
  const { data, isPending, error, refetch } = useDashboard();
  const canWrite = useCanWrite();

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Overview of guest sentiment, topics and ratings."
        actions={
          canWrite && (
            <Link href="/reviews/submit" className={buttonVariants()}>
              <PlusIcon />
              Submit review
            </Link>
          )
        }
      />

      {error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
          <Skeleton className="h-72 sm:col-span-2 lg:col-span-4" />
        </div>
      ) : (
        <div className="space-y-4">
          {data.unansweredNegative > 0 && (
            <Alert>
              <MessageSquareWarningIcon />
              <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                <span>
                  <strong>{data.unansweredNegative}</strong> negative review{data.unansweredNegative === 1 ? " has" : "s have"} not been answered yet.
                </span>
                <Link href="/reviews?sentiment=NEGATIVE" className={buttonVariants({ variant: "outline", size: "sm" })}>
                  Review them
                </Link>
              </AlertDescription>
            </Alert>
          )}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <MetricCard label="Total reviews" value={data.totalReviews} icon={MessageSquareTextIcon} />
            <MetricCard label="Average rating" value={data.averageRating.toFixed(2)} hint="out of 5" icon={StarIcon} />
            <MetricCard label="Top topic" value={humanize(data.mostCommonTopic)} icon={TagIcon} />
            <MetricCard
              label="Positive share"
              value={`${Math.round((data.sentimentCounts.POSITIVE / Math.max(1, data.totalReviews)) * 100)}%`}
              hint={`Most common rating: ${data.mostCommonRating}${data.mostCommonRating === "N/A" ? "" : "★"}`}
              icon={TrendingUpIcon}
            />
          </div>
          <div className="grid gap-4 lg:grid-cols-3">
            <SentimentChart counts={data.sentimentCounts} />
            <div className="lg:col-span-2">
              <TopicChart counts={data.topicCounts} />
            </div>
          </div>
          <WeeklyTrendCard />
          <div className="grid gap-4 lg:grid-cols-2">
            <RatingChart counts={data.ratingCounts} />
            <RecentReviews />
          </div>
        </div>
      )}
    </>
  );
}
