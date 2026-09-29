"use client";

import { CheckCircle2Icon, RefreshCwIcon, XCircleIcon } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { dataSourceMode } from "@/lib/data";
import { useAiStatus } from "@/lib/data/hooks";

function Check({ ok, label }: { ok: boolean; label: string }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border p-4">
      {ok ? <CheckCircle2Icon className="size-5 text-emerald-600" /> : <XCircleIcon className="size-5 text-destructive" />}
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{ok ? "OK" : "Unavailable"}</p>
      </div>
    </div>
  );
}

export function AiStatusView() {
  const { data, isPending, isFetching, error, refetch } = useAiStatus();

  return (
    <>
      <PageHeader
        title="AI status"
        description="Connectivity of the LLM runtime used for review analysis."
        actions={
          <Button variant="outline" onClick={() => refetch()} disabled={isFetching}>
            <RefreshCwIcon className={isFetching ? "animate-spin" : undefined} />
            Refresh
          </Button>
        }
      />

      {error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <Skeleton className="h-64 max-w-3xl" />
      ) : (
        <div className="max-w-3xl space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>{data.provider === "openai" ? "OpenAI-compatible API" : "Ollama"}</CardTitle>
              <CardDescription>{data.message}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <Check ok={data.reachable} label="Runtime reachable" />
                <Check ok={data.modelAvailable} label="Chat model available" />
              </div>
              <dl className="grid gap-2 text-sm sm:grid-cols-[140px_1fr]">
                <dt className="text-muted-foreground">Provider</dt>
                <dd className="font-mono text-xs">{data.provider}</dd>
                <dt className="text-muted-foreground">Base URL</dt>
                <dd className="font-mono text-xs">{data.baseUrl}</dd>
                <dt className="text-muted-foreground">Chat model</dt>
                <dd className="font-mono text-xs">{data.model}</dd>
              </dl>
            </CardContent>
          </Card>

          {dataSourceMode === "mock" && (
            <Alert>
              <AlertTitle>Running without a backend</AlertTitle>
              <AlertDescription>
                This deployment uses in-browser mock data. To connect the Spring Boot API, set{" "}
                <code>NEXT_PUBLIC_DATA_SOURCE=api</code> and <code>BACKEND_URL</code>, then redeploy.
              </AlertDescription>
            </Alert>
          )}
        </div>
      )}
    </>
  );
}
