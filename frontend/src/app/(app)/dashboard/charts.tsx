"use client";

import { Bar, BarChart, CartesianGrid, ComposedChart, Line, Pie, PieChart, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { type ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { formatDate, humanize } from "@/lib/format";
import type { DashboardMetrics, WeeklyTrend } from "@/lib/types";

const sentimentConfig = {
  count: { label: "Reviews" },
  POSITIVE: { label: "Positive", color: "var(--chart-1)" },
  NEUTRAL: { label: "Neutral", color: "var(--chart-2)" },
  NEGATIVE: { label: "Negative", color: "var(--chart-3)" },
} satisfies ChartConfig;

const countConfig = { count: { label: "Reviews", color: "var(--chart-1)" } } satisfies ChartConfig;
const ratingConfig = { count: { label: "Reviews", color: "var(--chart-2)" } } satisfies ChartConfig;

function EmptyChart() {
  return <p className="flex h-[240px] items-center justify-center text-sm text-muted-foreground">No data yet.</p>;
}

export function SentimentChart({ counts }: { counts: DashboardMetrics["sentimentCounts"] }) {
  const data = (Object.keys(counts) as (keyof typeof counts)[]).map((sentiment) => ({
    sentiment,
    count: counts[sentiment],
    fill: `var(--color-${sentiment})`,
  }));
  const total = data.reduce((sum, d) => sum + d.count, 0);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Sentiment</CardTitle>
        <CardDescription>AI-classified guest sentiment</CardDescription>
      </CardHeader>
      <CardContent>
        {total === 0 ? (
          <EmptyChart />
        ) : (
          <>
            <ChartContainer
              config={sentimentConfig}
              className="mx-auto aspect-square h-[220px]"
              role="img"
              aria-label={`Sentiment: ${data.map((d) => `${sentimentConfig[d.sentiment].label} ${d.count}`).join(", ")}`}
            >
              <PieChart>
                <ChartTooltip content={<ChartTooltipContent nameKey="sentiment" hideLabel />} />
                <Pie data={data} dataKey="count" nameKey="sentiment" innerRadius={55} strokeWidth={4} />
              </PieChart>
            </ChartContainer>
            <div className="mt-2 flex justify-center gap-4 text-xs">
              {data.map((d) => (
                <span key={d.sentiment} className="inline-flex items-center gap-1.5">
                  <span className="size-2.5 rounded-sm" style={{ background: sentimentConfig[d.sentiment].color }} />
                  {sentimentConfig[d.sentiment].label} ({d.count})
                </span>
              ))}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}

export function TopicChart({ counts }: { counts: DashboardMetrics["topicCounts"] }) {
  const data = Object.entries(counts).map(([topic, count]) => ({ topic: humanize(topic), count: count ?? 0 }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Main topics</CardTitle>
        <CardDescription>What guests talk about most</CardDescription>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <EmptyChart />
        ) : (
          <ChartContainer
            config={countConfig}
            className="h-[260px] w-full aspect-auto"
            role="img"
            aria-label={`Main topics: ${data.map((d) => `${d.topic} ${d.count}`).join(", ")}`}
          >
            <BarChart data={data} layout="vertical" margin={{ left: 8, right: 16 }}>
              <CartesianGrid horizontal={false} />
              <XAxis type="number" allowDecimals={false} hide />
              <YAxis type="category" dataKey="topic" width={100} interval={0} tickLine={false} axisLine={false} />
              <ChartTooltip cursor={false} content={<ChartTooltipContent hideLabel />} />
              <Bar dataKey="count" fill="var(--color-count)" radius={4} />
            </BarChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
}

export function TrendChart({ trends }: { trends: WeeklyTrend[] }) {
  const data = trends.map((t) => ({
    week: formatDate(t.weekStart).replace(/ \d{4}$/, ""),
    POSITIVE: t.positive,
    NEUTRAL: t.neutral,
    NEGATIVE: t.negative,
    averageRating: t.averageRating,
  }));
  const total = trends.reduce((sum, t) => sum + t.total, 0);
  const summary = trends
    .filter((t) => t.total > 0)
    .map((t) => `week of ${t.weekStart}: ${t.positive} positive, ${t.neutral} neutral, ${t.negative} negative`)
    .join("; ");

  return (
    <Card>
      <CardHeader>
        <CardTitle>Weekly trend</CardTitle>
        <CardDescription>Sentiment per week and average rating (last {trends.length} weeks)</CardDescription>
      </CardHeader>
      <CardContent>
        {total === 0 ? (
          <EmptyChart />
        ) : (
          <ChartContainer
            config={{ ...sentimentConfig, averageRating: { label: "Avg rating", color: "var(--chart-4)" } }}
            className="h-[280px] w-full aspect-auto"
            role="img"
            aria-label={`Weekly trend: ${summary}`}
          >
            <ComposedChart data={data} margin={{ left: 0, right: 8 }}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="week" tickLine={false} axisLine={false} interval="preserveStartEnd" />
              <YAxis yAxisId="count" allowDecimals={false} width={28} tickLine={false} axisLine={false} />
              <YAxis yAxisId="rating" orientation="right" domain={[1, 5]} width={24} tickLine={false} axisLine={false} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar yAxisId="count" dataKey="POSITIVE" stackId="s" fill="var(--color-POSITIVE)" />
              <Bar yAxisId="count" dataKey="NEUTRAL" stackId="s" fill="var(--color-NEUTRAL)" />
              <Bar yAxisId="count" dataKey="NEGATIVE" stackId="s" fill="var(--color-NEGATIVE)" radius={[4, 4, 0, 0]} />
              <Line yAxisId="rating" dataKey="averageRating" stroke="var(--color-averageRating)" strokeWidth={2} dot={false} connectNulls />
            </ComposedChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
}

export function RatingChart({ counts }: { counts: DashboardMetrics["ratingCounts"] }) {
  const data = Object.entries(counts).map(([rating, count]) => ({ rating: `${rating}★`, count }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Rating distribution</CardTitle>
        <CardDescription>Star ratings submitted by guests</CardDescription>
      </CardHeader>
      <CardContent>
        <ChartContainer
          config={ratingConfig}
          className="h-[260px] w-full aspect-auto"
          role="img"
          aria-label={`Ratings: ${data.map((d) => `${d.rating} ${d.count}`).join(", ")}`}
        >
          <BarChart data={data}>
            <CartesianGrid vertical={false} />
            <XAxis dataKey="rating" tickLine={false} axisLine={false} />
            <YAxis allowDecimals={false} width={28} tickLine={false} axisLine={false} />
            <ChartTooltip cursor={false} content={<ChartTooltipContent hideLabel />} />
            <Bar dataKey="count" fill="var(--color-count)" radius={4} />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
