"use client";

import { CheckIcon, CopyIcon, HistoryIcon, PencilIcon, SaveIcon, SendIcon, XIcon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useCanWrite, useReply, useReplyAction } from "@/lib/data/hooks";
import { formatDateTime, humanize } from "@/lib/format";
import type { ReplyStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATUS_STYLES: Record<ReplyStatus, string> = {
  DRAFT: "bg-muted text-muted-foreground",
  EDITED: "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300",
  APPROVED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
  SENT: "bg-violet-100 text-violet-800 dark:bg-violet-950 dark:text-violet-300",
};

export function ReplyPanel({ reviewId }: { reviewId: number }) {
  const { data: reply, isPending } = useReply(reviewId, true);
  const action = useReplyAction(reviewId);
  const canWrite = useCanWrite();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const [showHistory, setShowHistory] = useState(false);

  if (isPending || !reply) return <Skeleton className="h-32 w-full" />;

  const run = (input: Parameters<typeof action.mutate>[0], message: string) =>
    action.mutate(input, {
      onSuccess: () => {
        toast.success(message);
        setEditing(false);
      },
      onError: (e) => toast.error(e.message),
    });

  const copy = async () => {
    await navigator.clipboard.writeText(reply.text);
    toast.success("Reply copied.");
  };

  return (
    <div className="rounded-lg border bg-primary/5 p-4">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <p className="text-sm font-medium">Manager reply</p>
          <Badge className={cn(STATUS_STYLES[reply.status])}>{humanize(reply.status)}</Badge>
        </div>
        <Button variant="ghost" size="xs" onClick={copy}>
          <CopyIcon />
          Copy
        </Button>
      </div>

      {editing ? (
        <div className="space-y-2">
          <Textarea aria-label="Reply text" rows={6} maxLength={4000} value={draft} onChange={(e) => setDraft(e.target.value)} />
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={() => setEditing(false)}>
              <XIcon />
              Cancel
            </Button>
            <Button size="sm" disabled={action.isPending || !draft.trim()} onClick={() => run({ type: "edit", text: draft }, "Reply saved.")}>
              <SaveIcon />
              Save
            </Button>
          </div>
        </div>
      ) : (
        <p className="whitespace-pre-line text-sm leading-relaxed">{reply.text}</p>
      )}

      <p className="mt-2 text-xs text-muted-foreground">
        {reply.status === "DRAFT" ? "AI suggestion" : `${humanize(reply.status)} by ${reply.updatedBy} · ${formatDateTime(reply.updatedAt)}`}
      </p>

      {canWrite && !editing && (
        <div className="mt-3 flex flex-wrap gap-2">
          {reply.status !== "SENT" && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setDraft(reply.text);
                setEditing(true);
              }}
            >
              <PencilIcon />
              Edit
            </Button>
          )}
          {(reply.status === "DRAFT" || reply.status === "EDITED") && (
            <Button size="sm" disabled={action.isPending} onClick={() => run({ type: "approve" }, "Reply approved.")}>
              <CheckIcon />
              Approve
            </Button>
          )}
          {reply.status === "APPROVED" && (
            <Button size="sm" disabled={action.isPending} onClick={() => run({ type: "sent" }, "Marked as sent.")}>
              <SendIcon />
              Mark as sent
            </Button>
          )}
        </div>
      )}

      {reply.history.length > 0 && (
        <div className="mt-3 border-t pt-2">
          <Button variant="ghost" size="xs" onClick={() => setShowHistory((v) => !v)} aria-expanded={showHistory}>
            <HistoryIcon />
            {showHistory ? "Hide" : "Show"} history ({reply.history.length})
          </Button>
          {showHistory && (
            <ol className="mt-2 space-y-2 text-xs">
              {[...reply.history].reverse().map((revision, index) => (
                <li key={`${revision.createdAt}-${index}`} className="rounded-md border bg-background p-2">
                  <div className="mb-1 flex justify-between text-muted-foreground">
                    <span>
                      {humanize(revision.status)} by {revision.author}
                    </span>
                    <span>{formatDateTime(revision.createdAt)}</span>
                  </div>
                  <p className="line-clamp-3">{revision.text}</p>
                </li>
              ))}
            </ol>
          )}
        </div>
      )}
    </div>
  );
}
