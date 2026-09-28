"use client";

import { ArrowLeftIcon, BookOpenIcon, CopyIcon, Loader2Icon, RotateCwIcon, SparklesIcon } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";
import { RatingStars, SentimentBadge, StatusBadge } from "@/components/badges";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { isNotFound } from "@/lib/data";
import { useRetryAnalysis, useReview } from "@/lib/data/hooks";
import { formatDateTime, humanize } from "@/lib/format";
import type { Review } from "@/lib/types";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-1.5 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right">{children}</span>
    </div>
  );
}

function ScoreBar({ score }: { score: number }) {
  const color = score >= 65 ? "bg-emerald-500" : score <= 35 ? "bg-red-500" : "bg-amber-500";
  return (
    <div className="flex items-center gap-2">
      <div className="h-2 w-28 overflow-hidden rounded-full bg-muted">
        <div className={`h-full ${color}`} style={{ width: `${score}%` }} />
      </div>
      <span className="tabular-nums">{score}</span>
    </div>
  );
}

function GuestReviewCard({ review }: { review: Review }) {
  const retry = useRetryAnalysis();

  return (
    <Card>
      <CardHeader>
        <CardTitle>Guest review</CardTitle>
        <CardDescription>Submitted {formatDateTime(review.submittedAt)}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="divide-y">
          <Field label="Guest">{review.guestName}</Field>
          <Field label="Rating">
            <RatingStars rating={review.rating} />
          </Field>
          <Field label="AI status">
            <StatusBadge status={review.analysisStatus} />
          </Field>
          <Field label="Last analysis update">{formatDateTime(review.analysisUpdatedAt)}</Field>
        </div>
        <blockquote className="whitespace-pre-line rounded-lg border-l-4 border-primary/40 bg-muted/50 p-4 text-sm leading-relaxed">
          {review.reviewText}
        </blockquote>
        {review.analysisStatus === "FAILED" && (
          <Alert variant="destructive">
            <AlertTitle>Analysis failed</AlertTitle>
            <AlertDescription className="space-y-3">
              <p>{review.analysisError ?? "Unexpected analysis error."}</p>
              <Button
                variant="outline"
                size="sm"
                disabled={retry.isPending}
                onClick={() => retry.mutate(review.id, { onSuccess: () => toast.success("Analysis retry queued.") })}
              >
                <RotateCwIcon />
                Retry analysis
              </Button>
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  );
}

function AnalysisCard({ review }: { review: Review }) {
  const analysis = review.analysis;
  const copy = async () => {
    if (!analysis) return;
    await navigator.clipboard.writeText(analysis.managerResponse);
    toast.success("Manager response copied.");
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <SparklesIcon className="size-4 text-primary" />
          AI analysis
        </CardTitle>
        <CardDescription>Structured output from the LLM</CardDescription>
      </CardHeader>
      <CardContent>
        {!analysis ? (
          <div className="flex flex-col items-center justify-center gap-2 py-10 text-center text-sm text-muted-foreground">
            {review.analysisStatus === "PENDING" || review.analysisStatus === "PROCESSING" ? (
              <>
                <Loader2Icon className="size-6 animate-spin text-primary" />
                Analysis in progress ({humanize(review.analysisStatus)})…
              </>
            ) : (
              <>Analysis is not available. Current status: {humanize(review.analysisStatus)}</>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            <div className="divide-y">
              <Field label="Sentiment">
                <SentimentBadge sentiment={analysis.sentiment} />
              </Field>
              <Field label="Sentiment score">
                <ScoreBar score={analysis.sentimentScore} />
              </Field>
              <Field label="Main topic">{humanize(analysis.mainTopic)}</Field>
              <Field label="Topics">
                <span className="flex flex-wrap justify-end gap-1">
                  {analysis.topics.map((t) => (
                    <Badge key={t} variant="outline">
                      {humanize(t)}
                    </Badge>
                  ))}
                </span>
              </Field>
            </div>
            <div className="rounded-lg border bg-primary/5 p-4">
              <div className="mb-2 flex items-center justify-between">
                <p className="text-sm font-medium">Suggested manager response</p>
                <Button variant="ghost" size="xs" onClick={copy}>
                  <CopyIcon />
                  Copy
                </Button>
              </div>
              <p className="text-sm leading-relaxed">{analysis.managerResponse}</p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export function ReviewDetailView({ id }: { id: number }) {
  const { data, isPending, error, refetch } = useReview(id);
  const inFlight = data && (data.review.analysisStatus === "PENDING" || data.review.analysisStatus === "PROCESSING");

  const back = (
    <Link href="/reviews" className={buttonVariants({ variant: "outline" })}>
      <ArrowLeftIcon />
      Back to reviews
    </Link>
  );

  if (error) {
    return (
      <>
        <PageHeader title="Review detail" actions={back} />
        {isNotFound(error) ? (
          <Card>
            <CardContent className="py-10 text-center text-muted-foreground">Review #{id} was not found.</CardContent>
          </Card>
        ) : (
          <QueryError error={error} onRetry={() => refetch()} />
        )}
      </>
    );
  }

  return (
    <>
      <PageHeader title="Review detail" description="Guest feedback with AI analysis and policy context." actions={back} />
      {isPending ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <Skeleton className="h-80" />
          <Skeleton className="h-80" />
          <Skeleton className="h-40 lg:col-span-2" />
        </div>
      ) : (
        <div className="space-y-4">
          {inFlight && (
            <Alert>
              <Loader2Icon className="animate-spin" />
              <AlertDescription>Analysis is in progress. This page updates automatically.</AlertDescription>
            </Alert>
          )}
          <div className="grid gap-4 lg:grid-cols-2">
            <GuestReviewCard review={data.review} />
            <AnalysisCard review={data.review} />
          </div>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BookOpenIcon className="size-4 text-primary" />
                Policy context (RAG)
              </CardTitle>
              <CardDescription>Hotel policies retrieved from pgvector to ground the manager response</CardDescription>
              <CardAction>
                <Badge variant={data.ragEnabled ? "default" : "outline"}>{data.ragEnabled ? "Enabled" : "Disabled"}</Badge>
              </CardAction>
            </CardHeader>
            <CardContent>
              {data.ragEnabled ? (
                <pre className="overflow-x-auto whitespace-pre-wrap rounded-lg bg-muted/50 p-4 font-mono text-xs leading-relaxed">
                  {data.policyContext}
                </pre>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Policy context is not available. Enable pgvector and set <code>app.rag.enabled=true</code>.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
}
