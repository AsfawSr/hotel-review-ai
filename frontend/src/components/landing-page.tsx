import {
  ArrowRightIcon,
  BookOpenIcon,
  BrainCircuitIcon,
  DatabaseIcon,
  GaugeIcon,
  HotelIcon,
  LayersIcon,
  MessageSquareReplyIcon,
  ServerIcon,
  ShieldCheckIcon,
  TagsIcon,
  TimerResetIcon,
} from "lucide-react";
import Link from "next/link";
import { ThemeToggle } from "@/components/theme-toggle";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

const GITHUB_URL = "https://github.com/AsfawSr/hotel-review-ai";

function GithubIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden className={className}>
      <path d="M12 .5a11.5 11.5 0 0 0-3.64 22.41c.58.1.79-.25.79-.56v-2c-3.2.7-3.88-1.37-3.88-1.37-.52-1.33-1.28-1.69-1.28-1.69-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.55-.29-5.24-1.28-5.24-5.69 0-1.26.45-2.29 1.19-3.1-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.77 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.84 1.19 3.1 0 4.42-2.7 5.39-5.26 5.68.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 12 .5Z" />
    </svg>
  );
}

const FEATURES = [
  { icon: GaugeIcon, title: "Sentiment scoring", text: "Every review gets a POSITIVE / NEUTRAL / NEGATIVE label and a 0–100 score." },
  { icon: TagsIcon, title: "Topic detection", text: "13 hospitality topics — cleanliness, staff, food, noise, check-in and more." },
  { icon: MessageSquareReplyIcon, title: "Manager responses", text: "A polite, personalized reply draft generated for each guest." },
  { icon: BookOpenIcon, title: "RAG policy grounding", text: "Relevant hotel policies are retrieved from pgvector to ground every reply." },
  { icon: TimerResetIcon, title: "Async + fallback", text: "Analysis runs on a background pool with retry and a heuristic fallback." },
  { icon: ShieldCheckIcon, title: "Secure dashboard", text: "Authenticated analytics with filters, pagination and live status updates." },
];

const PIPELINE = [
  { title: "Guest review", text: "Submitted via the dashboard" },
  { title: "Async queue", text: "@Async worker pool" },
  { title: "RAG retrieval", text: "pgvector similarity search" },
  { title: "LLM analysis", text: "Spring AI → Ollama" },
  { title: "Structured JSON", text: "Validated & stored" },
];

const STACK: Record<string, string[]> = {
  Backend: ["Java 21", "Spring Boot 3.4", "Spring Security", "Spring Data JPA", "PostgreSQL"],
  AI: ["Spring AI 1.0", "Ollama", "llama3.2", "nomic-embed-text", "pgvector (HNSW)"],
  Frontend: ["Next.js 16", "React 19", "TypeScript", "Tailwind CSS v4", "shadcn/ui", "TanStack Query", "Recharts"],
  Deployment: ["Vercel", "Maven", "GitHub"],
};

function ArchBox({ icon: Icon, title, items, className }: { icon: React.ElementType; title: string; items: string[]; className?: string }) {
  return (
    <div className={cn("rounded-xl border bg-card p-4 shadow-sm", className)}>
      <div className="mb-2 flex items-center gap-2 font-medium">
        <Icon className="size-4 text-primary" />
        {title}
      </div>
      <ul className="space-y-1 text-xs text-muted-foreground">
        {items.map((i) => (
          <li key={i}>{i}</li>
        ))}
      </ul>
    </div>
  );
}

function Arrow() {
  return <ArrowRightIcon className="mx-auto size-5 rotate-90 text-muted-foreground lg:rotate-0" />;
}

function PreviewCard() {
  return (
    <Card className="w-full max-w-md shadow-xl">
      <CardHeader>
        <CardDescription>Guest review · ★★☆☆☆</CardDescription>
        <CardTitle className="text-sm font-normal leading-relaxed">
          &ldquo;The room next to ours had a party until 2am and nobody from security came despite two calls.&rdquo;
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        <div className="flex flex-wrap items-center gap-2">
          <Badge className="bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300">Negative · 18</Badge>
          <Badge variant="outline">Noise</Badge>
          <Badge variant="outline">Safety</Badge>
        </div>
        <div className="rounded-lg border bg-primary/5 p-3 text-xs leading-relaxed">
          <p className="mb-1 font-medium">Suggested manager response</p>
          Dear Liam, I sincerely apologise that noise levels did not meet your expectations. Per our quiet-hours policy,
          security should respond within 15 minutes…
        </div>
      </CardContent>
    </Card>
  );
}

export default function LandingPage() {
  return (
    <div className="flex flex-1 flex-col bg-background">
      <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4 sm:px-6">
          <Link href="/" className="flex items-center gap-2 font-semibold text-primary">
            <HotelIcon className="size-5" />
            HotelReviewAI
          </Link>
          <nav className="hidden items-center gap-5 text-sm text-muted-foreground md:flex">
            <a href="#features" className="hover:text-foreground">Features</a>
            <a href="#how-it-works" className="hover:text-foreground">How it works</a>
            <a href="#stack" className="hover:text-foreground">Tech stack</a>
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className={buttonVariants({ variant: "ghost", size: "icon" })} aria-label="GitHub repository">
              <GithubIcon className="size-4" />
            </a>
            <Link href="/login" className={buttonVariants()}>
              Try the demo
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1">
        <section className="bg-gradient-to-b from-primary/10 to-background">
          <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
            <div>
              <Badge variant="outline" className="mb-4">
                <BrainCircuitIcon />
                Spring AI · RAG · Next.js
              </Badge>
              <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">
                Turn guest reviews into <span className="text-primary">actionable insight</span>
              </h1>
              <p className="mt-4 max-w-xl text-lg text-muted-foreground">
                HotelReviewAI analyzes hotel guest feedback with an LLM — scoring sentiment, detecting topics and drafting
                policy-grounded manager responses — then surfaces trends on a live dashboard.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Link href="/login" className={buttonVariants({ size: "lg" })}>
                  Try the live demo
                  <ArrowRightIcon />
                </Link>
                <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className={buttonVariants({ variant: "outline", size: "lg" })}>
                  <GithubIcon className="size-4" />
                  View source
                </a>
              </div>
              <p className="mt-3 text-xs text-muted-foreground">No sign-up needed — demo credentials are pre-filled.</p>
            </div>
            <div className="flex justify-center lg:justify-end">
              <PreviewCard />
            </div>
          </div>
        </section>

        <section id="features" className="mx-auto max-w-6xl scroll-mt-16 px-4 py-16 sm:px-6">
          <h2 className="text-2xl font-semibold tracking-tight">Features</h2>
          <p className="mt-1 text-muted-foreground">Everything a hotel team needs to understand and respond to feedback.</p>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <Card key={title}>
                <CardHeader>
                  <span className="mb-2 flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icon className="size-4" />
                  </span>
                  <CardTitle>{title}</CardTitle>
                  <CardDescription>{text}</CardDescription>
                </CardHeader>
              </Card>
            ))}
          </div>
        </section>

        <section id="how-it-works" className="scroll-mt-16 border-y bg-muted/40">
          <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
            <h2 className="text-2xl font-semibold tracking-tight">How it works</h2>
            <p className="mt-1 text-muted-foreground">The analysis pipeline, from submission to dashboard.</p>

            <ol className="mt-8 grid gap-3 lg:grid-cols-5">
              {PIPELINE.map((step, i) => (
                <li key={step.title} className="relative rounded-xl border bg-card p-4">
                  <span className="mb-2 flex size-6 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                    {i + 1}
                  </span>
                  <p className="font-medium">{step.title}</p>
                  <p className="text-xs text-muted-foreground">{step.text}</p>
                </li>
              ))}
            </ol>

            <h3 className="mt-12 mb-4 font-semibold">Architecture</h3>
            <div className="grid items-center gap-3 lg:grid-cols-[1fr_auto_1.2fr_auto_1fr]">
              <ArchBox icon={LayersIcon} title="Next.js frontend" items={["Hosted on Vercel", "App Router + TanStack Query", "Mock or API data source"]} />
              <Arrow />
              <ArchBox
                icon={ServerIcon}
                title="Spring Boot backend"
                className="border-primary/40"
                items={["Spring Security auth", "Async analysis workers", "Spring AI ChatClient + structured output"]}
              />
              <Arrow />
              <div className="grid gap-3">
                <ArchBox icon={DatabaseIcon} title="PostgreSQL + pgvector" items={["Reviews & analyses", "Policy embeddings (HNSW)"]} />
                <ArchBox icon={BrainCircuitIcon} title="Ollama" items={["llama3.2 (chat)", "nomic-embed-text (embeddings)"]} />
              </div>
            </div>
          </div>
        </section>

        <section id="stack" className="mx-auto max-w-6xl scroll-mt-16 px-4 py-16 sm:px-6">
          <h2 className="text-2xl font-semibold tracking-tight">Tech stack</h2>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Object.entries(STACK).map(([group, items]) => (
              <Card key={group} size="sm">
                <CardHeader>
                  <CardTitle>{group}</CardTitle>
                </CardHeader>
                <CardContent className="flex flex-wrap gap-1.5">
                  {items.map((i) => (
                    <Badge key={i} variant="secondary">
                      {i}
                    </Badge>
                  ))}
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
          <div className="flex flex-col items-center gap-4 rounded-2xl bg-primary px-6 py-12 text-center text-primary-foreground">
            <h2 className="text-2xl font-semibold">See it in action</h2>
            <p className="max-w-lg text-primary-foreground">
              Submit a review and watch it move from Pending to Completed with sentiment, topics and a drafted reply.
            </p>
            <Link href="/login" className={buttonVariants({ variant: "secondary", size: "lg" })}>
              Open the demo dashboard
              <ArrowRightIcon />
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t py-6 text-center text-xs text-muted-foreground">
        HotelReviewAI — built with Spring Boot, Spring AI and Next.js ·{" "}
        <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className="underline-offset-4 hover:underline">
          GitHub
        </a>
      </footer>
    </div>
  );
}
