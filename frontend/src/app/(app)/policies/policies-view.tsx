"use client";

import { PencilIcon, PlusIcon, RefreshCwIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useDeletePolicy, useIsAdmin, usePolicies, useReindexPolicies } from "@/lib/data/hooks";
import { formatDate } from "@/lib/format";
import type { Policy } from "@/lib/types";
import { PolicyForm } from "./policy-form";

type Editing = { mode: "create" } | { mode: "edit"; policy: Policy } | null;

export function PoliciesView() {
  const { data, isPending, error, refetch } = usePolicies();
  const isAdmin = useIsAdmin();
  const remove = useDeletePolicy();
  const reindex = useReindexPolicies();
  const [editing, setEditing] = useState<Editing>(null);

  const handleDelete = (policy: Policy) => {
    if (!window.confirm(`Delete "${policy.title}"? It will no longer be used to ground responses.`)) return;
    remove.mutate(policy.id, { onSuccess: () => toast.success("Policy deleted.") });
  };

  const handleReindex = () =>
    reindex.mutate(undefined, {
      onSuccess: (result) =>
        result.ragEnabled
          ? toast.success(`Re-indexed ${result.indexed} active policies.`)
          : toast.warning("RAG is disabled on the backend; enable the 'rag' profile to use the vector store."),
      onError: (e) => toast.error(e.message),
    });

  return (
    <>
      <PageHeader
        title="Hotel policies"
        description="Knowledge base retrieved (RAG) to ground AI manager responses."
        actions={
          isAdmin && (
            <>
              <Button variant="outline" onClick={handleReindex} disabled={reindex.isPending}>
                <RefreshCwIcon className={reindex.isPending ? "animate-spin" : undefined} />
                Re-index
              </Button>
              <Button onClick={() => setEditing({ mode: "create" })}>
                <PlusIcon />
                New policy
              </Button>
            </>
          )
        }
      />

      {editing && (
        <PolicyForm
          key={editing.mode === "edit" ? editing.policy.id : "new"}
          policy={editing.mode === "edit" ? editing.policy : undefined}
          onDone={() => setEditing(null)}
        />
      )}

      {error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-44" />
          ))}
        </div>
      ) : data.length === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-muted-foreground">No policies yet.</CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {data.map((policy) => (
            <Card key={policy.id} className={policy.active ? undefined : "opacity-60"}>
              <CardHeader>
                <CardTitle>{policy.title}</CardTitle>
                <CardDescription className="flex flex-wrap items-center gap-2">
                  <Badge variant="secondary">{policy.category}</Badge>
                  {!policy.active && <Badge variant="outline">Inactive</Badge>}
                  {policy.effectiveDate && <span>Effective {formatDate(policy.effectiveDate)}</span>}
                </CardDescription>
                {isAdmin && (
                  <CardAction className="flex gap-1">
                    <Button variant="ghost" size="icon-sm" aria-label={`Edit ${policy.title}`} onClick={() => setEditing({ mode: "edit", policy })}>
                      <PencilIcon />
                    </Button>
                    <Button variant="ghost" size="icon-sm" aria-label={`Delete ${policy.title}`} onClick={() => handleDelete(policy)} disabled={remove.isPending}>
                      <Trash2Icon />
                    </Button>
                  </CardAction>
                )}
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-sm leading-relaxed text-muted-foreground">{policy.content}</p>
                <div className="flex flex-wrap items-center gap-1.5">
                  {policy.tags.map((tag) => (
                    <Badge key={tag} variant="outline">
                      {tag}
                    </Badge>
                  ))}
                  {policy.source && <span className="ml-auto text-xs text-muted-foreground">{policy.source}</span>}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
