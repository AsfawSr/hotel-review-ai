"use client";

import { SaveIcon, XIcon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/data";
import { useSavePolicy } from "@/lib/data/hooks";
import type { Policy, PolicyInput } from "@/lib/types";
import { POLICY_LIMITS, validatePolicy } from "@/lib/validation";

function FieldError({ message }: { message?: string }) {
  return message ? <p className="text-xs text-destructive">{message}</p> : null;
}

export function PolicyForm({ policy, onDone }: { policy?: Policy; onDone: () => void }) {
  const save = useSavePolicy();
  const [title, setTitle] = useState(policy?.title ?? "");
  const [category, setCategory] = useState(policy?.category ?? "");
  const [content, setContent] = useState(policy?.content ?? "");
  const [tags, setTags] = useState(policy?.tags.join(", ") ?? "");
  const [source, setSource] = useState(policy?.source ?? "");
  const [effectiveDate, setEffectiveDate] = useState(policy?.effectiveDate ?? "");
  const [active, setActive] = useState(policy?.active ?? true);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const input: PolicyInput = {
      title,
      category,
      content,
      tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
      source: source.trim() || null,
      effectiveDate: effectiveDate || null,
      active,
    };
    const clientErrors = validatePolicy(input);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length) return;

    save.mutate(
      { id: policy?.id, input },
      {
        onSuccess: () => {
          toast.success(policy ? "Policy updated and queued for re-embedding." : "Policy created and queued for embedding.");
          onDone();
        },
        onError: (error) => {
          if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
          else toast.error(error.message);
        },
      },
    );
  };

  return (
    <Card className="mb-4 border-primary/30">
      <CardHeader>
        <CardTitle>{policy ? "Edit policy" : "New policy"}</CardTitle>
        <CardDescription>Active policies are embedded into the vector store and retrieved to ground manager responses.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="policy-title">Title</Label>
            <Input id="policy-title" value={title} maxLength={POLICY_LIMITS.title} aria-invalid={!!errors.title} onChange={(e) => setTitle(e.target.value)} />
            <FieldError message={errors.title} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="policy-category">Category</Label>
            <Input id="policy-category" value={category} maxLength={POLICY_LIMITS.category} aria-invalid={!!errors.category} onChange={(e) => setCategory(e.target.value)} placeholder="e.g. Front Office" />
            <FieldError message={errors.category} />
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="policy-content">Content</Label>
              <span className="text-xs text-muted-foreground">
                {content.length}/{POLICY_LIMITS.content}
              </span>
            </div>
            <Textarea id="policy-content" rows={5} value={content} maxLength={POLICY_LIMITS.content} aria-invalid={!!errors.content} onChange={(e) => setContent(e.target.value)} />
            <FieldError message={errors.content} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="policy-tags">Tags</Label>
            <Input id="policy-tags" value={tags} aria-invalid={!!errors.tags} onChange={(e) => setTags(e.target.value)} placeholder="noise, safety" />
            <FieldError message={errors.tags} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="policy-source">Source</Label>
            <Input id="policy-source" value={source} maxLength={POLICY_LIMITS.source} onChange={(e) => setSource(e.target.value)} placeholder="e.g. Operations Manual 4.2" />
            <FieldError message={errors.source} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="policy-effective">Effective date</Label>
            <Input id="policy-effective" type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
          </div>
          <label className="flex items-center gap-2 self-end pb-2 text-sm">
            <input type="checkbox" className="size-4 accent-primary" checked={active} onChange={(e) => setActive(e.target.checked)} />
            Active (used for retrieval)
          </label>
          <div className="flex justify-end gap-2 sm:col-span-2">
            <Button type="button" variant="outline" onClick={onDone}>
              <XIcon />
              Cancel
            </Button>
            <Button type="submit" disabled={save.isPending}>
              <SaveIcon />
              {save.isPending ? "Saving…" : "Save policy"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
