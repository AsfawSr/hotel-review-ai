"use client";

import { ChevronLeftIcon, ChevronRightIcon, EyeIcon, FilterIcon, PlusIcon, SearchIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { RatingStars, SentimentBadge, StatusBadge } from "@/components/badges";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { ALL, SimpleSelect } from "@/components/simple-select";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useCanWrite, useReviews } from "@/lib/data/hooks";
import { formatDateTime, humanize } from "@/lib/format";
import { ANALYSIS_STATUSES, PAGE_SIZES, type ReviewQuery, SENTIMENTS, TOPICS } from "@/lib/types";

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const FILTER_KEYS = ["q", "status", "sentiment", "topic", "ratingMin", "ratingMax", "dateFrom", "dateTo", "guest"] as const;
type FilterKey = (typeof FILTER_KEYS)[number];
type Draft = Record<FilterKey, string>;

const toOptions = (values: readonly string[]) => values.map((v) => ({ value: v, label: humanize(v) }));
const ratingOptions = [1, 2, 3, 4, 5].map((n) => ({ value: String(n), label: `${n}★` }));

function parseQuery(params: URLSearchParams): ReviewQuery {
  const pick = <T extends string>(key: string, allowed: readonly T[]) => {
    const v = params.get(key);
    return v && (allowed as readonly string[]).includes(v) ? (v as T) : undefined;
  };
  const int = (key: string) => {
    const n = Number(params.get(key));
    return params.has(key) && Number.isInteger(n) ? n : undefined;
  };
  const date = (key: string) => {
    const v = params.get(key);
    return v && DATE_RE.test(v) ? v : undefined;
  };
  const size = int("size");

  return {
    page: Math.max(0, int("page") ?? 0),
    size: size && (PAGE_SIZES as readonly number[]).includes(size) ? size : PAGE_SIZES[0],
    status: pick("status", ANALYSIS_STATUSES),
    sentiment: pick("sentiment", SENTIMENTS),
    topic: pick("topic", TOPICS),
    ratingMin: int("ratingMin"),
    ratingMax: int("ratingMax"),
    dateFrom: date("dateFrom"),
    dateTo: date("dateTo"),
    guest: params.get("guest")?.trim() || undefined,
    q: params.get("q")?.trim() || undefined,
  };
}

function toDraft(query: ReviewQuery): Draft {
  return Object.fromEntries(
    FILTER_KEYS.map((k) => [k, query[k] != null ? String(query[k]) : ["status", "sentiment", "topic", "ratingMin", "ratingMax"].includes(k) ? ALL : ""]),
  ) as Draft;
}

function FiltersForm({ query, onApply }: { query: ReviewQuery; onApply: (draft: Draft) => void }) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(query));
  const set = (key: FilterKey) => (value: string) => setDraft((d) => ({ ...d, [key]: value }));

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onApply(draft);
      }}
      className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8"
    >
      <div className="space-y-1.5 sm:col-span-2 lg:col-span-4 xl:col-span-8">
        <Label htmlFor="q">Search reviews</Label>
        <div className="relative">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
          <Input
            id="q"
            type="search"
            className="pl-8"
            placeholder="Keywords in the review text, e.g. breakfast cold"
            value={draft.q}
            onChange={(e) => set("q")(e.target.value)}
          />
        </div>
      </div>
      <div className="space-y-1.5 sm:col-span-2 lg:col-span-4 xl:col-span-2">
        <Label htmlFor="guest">Guest</Label>
        <Input id="guest" placeholder="Search by name" value={draft.guest} onChange={(e) => set("guest")(e.target.value)} />
      </div>
      <SimpleSelect id="status" label="Status" value={draft.status} onChange={set("status")} allLabel="All statuses" options={toOptions(ANALYSIS_STATUSES)} />
      <SimpleSelect id="sentiment" label="Sentiment" value={draft.sentiment} onChange={set("sentiment")} allLabel="All sentiments" options={toOptions(SENTIMENTS)} />
      <SimpleSelect id="topic" label="Main topic" value={draft.topic} onChange={set("topic")} allLabel="All topics" options={toOptions(TOPICS)} />
      <div className="grid grid-cols-2 gap-2">
        <SimpleSelect id="ratingMin" label="Min ★" value={draft.ratingMin} onChange={set("ratingMin")} allLabel="Any" options={ratingOptions} />
        <SimpleSelect id="ratingMax" label="Max ★" value={draft.ratingMax} onChange={set("ratingMax")} allLabel="Any" options={ratingOptions} />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="dateFrom">From</Label>
        <Input id="dateFrom" type="date" value={draft.dateFrom} onChange={(e) => set("dateFrom")(e.target.value)} />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="dateTo">To</Label>
        <Input id="dateTo" type="date" value={draft.dateTo} onChange={(e) => set("dateTo")(e.target.value)} />
      </div>
      <div className="flex items-end gap-2 sm:col-span-2 lg:col-span-4 xl:col-span-8 xl:justify-end">
        <Button type="submit">
          <FilterIcon />
          Apply filters
        </Button>
      </div>
    </form>
  );
}

export function ReviewsView() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const query = parseQuery(params);
  const { data, isPending, isFetching, error, refetch } = useReviews(query);
  const canWrite = useCanWrite();
  const hasFilters = FILTER_KEYS.some((k) => query[k] != null);

  const navigate = (next: Partial<Record<FilterKey | "page" | "size", string | number | undefined>>) => {
    const merged = new URLSearchParams(params);
    for (const [key, value] of Object.entries(next)) {
      if (value === undefined || value === "" || value === ALL) merged.delete(key);
      else merged.set(key, String(value));
    }
    if (merged.get("page") === "0") merged.delete("page");
    const qs = merged.toString();
    router.push(qs ? `${pathname}?${qs}` : pathname);
  };

  return (
    <>
      <PageHeader
        title="Reviews"
        description="Browse, filter and inspect analyzed guest reviews."
        actions={
          canWrite && (
            <Link href="/reviews/submit" className={buttonVariants()}>
              <PlusIcon />
              Submit review
            </Link>
          )
        }
      />

      <Card className="mb-4">
        <CardContent>
          <FiltersForm key={params.toString()} query={query} onApply={(draft) => navigate({ ...draft, page: undefined })} />
          {hasFilters && (
            <Button variant="ghost" size="sm" className="mt-2" onClick={() => router.push(pathname)}>
              <XIcon />
              Clear filters
            </Button>
          )}
        </CardContent>
      </Card>

      {error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : (
        <Card className="py-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Guest</TableHead>
                <TableHead>Rating</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Sentiment</TableHead>
                <TableHead>Main topic</TableHead>
                <TableHead>Submitted</TableHead>
                <TableHead className="pr-4 text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody className={isFetching && !isPending ? "opacity-60 transition-opacity" : undefined}>
              {isPending ? (
                Array.from({ length: query.size > 10 ? 10 : query.size }, (_, i) => (
                  <TableRow key={i}>
                    <TableCell colSpan={7} className="px-4">
                      <Skeleton className="h-6 w-full" />
                    </TableCell>
                  </TableRow>
                ))
              ) : data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="py-10 text-center text-muted-foreground">
                    No reviews match your filters.
                  </TableCell>
                </TableRow>
              ) : (
                data.content.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="pl-4 font-medium">
                      <Link href={`/reviews/${r.id}`} className="hover:underline">
                        {r.guestName}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <RatingStars rating={r.rating} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={r.analysisStatus} />
                    </TableCell>
                    <TableCell>
                      <SentimentBadge sentiment={r.sentiment} />
                    </TableCell>
                    <TableCell>{humanize(r.mainTopic)}</TableCell>
                    <TableCell className="text-muted-foreground">{formatDateTime(r.submittedAt)}</TableCell>
                    <TableCell className="pr-4 text-right">
                      <Link href={`/reviews/${r.id}`} className={buttonVariants({ variant: "outline", size: "sm" })}>
                        <EyeIcon />
                        View
                      </Link>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {data && (
            <div className="flex flex-wrap items-center justify-between gap-3 border-t px-4 py-3 text-sm">
              <span className="text-muted-foreground">
                {data.totalElements === 0
                  ? "0 reviews"
                  : `Showing ${data.page * data.size + 1}–${data.page * data.size + data.content.length} of ${data.totalElements}`}
              </span>
              <div className="flex items-center gap-3">
                <SimpleSelect
                  id="size"
                  ariaLabel="Reviews per page"
                  className="w-24"
                  value={String(query.size)}
                  onChange={(v) => navigate({ size: v === String(PAGE_SIZES[0]) ? undefined : v, page: undefined })}
                  options={PAGE_SIZES.map((s) => ({ value: String(s), label: `${s} / page` }))}
                />
                <span className="text-muted-foreground">
                  Page {data.page + 1} of {data.totalPages}
                </span>
                <Button variant="outline" size="icon-sm" aria-label="Previous page" disabled={data.page === 0} onClick={() => navigate({ page: data.page - 1 })}>
                  <ChevronLeftIcon />
                </Button>
                <Button
                  variant="outline"
                  size="icon-sm"
                  aria-label="Next page"
                  disabled={data.page + 1 >= data.totalPages}
                  onClick={() => navigate({ page: data.page + 1 })}
                >
                  <ChevronRightIcon />
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}
    </>
  );
}
