"use client";

import { SendIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { ALL, SimpleSelect } from "@/components/simple-select";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/data";
import { useSubmitReview } from "@/lib/data/hooks";
import { GUEST_NAME_MAX, REVIEW_TEXT_MAX, validateSubmission } from "@/lib/validation";
import { ImportReviewsCard } from "./import-reviews-card";

const ratingOptions = [5, 4, 3, 2, 1].map((n) => ({ value: String(n), label: `${"★".repeat(n)} (${n})` }));

function FieldError({ message }: { message?: string }) {
  return message ? <p className="text-xs text-destructive">{message}</p> : null;
}

export function SubmitReviewForm() {
  const router = useRouter();
  const submit = useSubmitReview();
  const [guestName, setGuestName] = useState("");
  const [rating, setRating] = useState<string>(ALL);
  const [reviewText, setReviewText] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const submission = { guestName, reviewText, rating: rating === ALL ? null : Number(rating) };
    const clientErrors = validateSubmission(submission);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length) return;

    submit.mutate(submission, {
      onSuccess: (review) => {
        toast.success("Review submitted. AI analysis has been queued.");
        router.push(`/reviews/${review.id}`);
      },
      onError: (error) => {
        if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
        else toast.error(error.message);
      },
    });
  };

  return (
    <> one by one or import a CSV. Everything is
      <PageHeader title="Submit review" description="Add guest feedback. It will be analyzed asynchronously by the AI pipeline." />
      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>Guest feedback</CardTitle>
          <CardDescription>Sentiment, topics and a manager response are generated automatically.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            <div className="grid gap-4 sm:grid-cols-[1fr_200px]">
              <div className="space-y-1.5">
                <Label htmlFor="guestName">Guest name</Label>
                <Input
                  id="guestName"
                  value={guestName}
                  maxLength={GUEST_NAME_MAX}
                  aria-invalid={!!errors.guestName}
                  onChange={(e) => setGuestName(e.target.value)}
                  placeholder="e.g. Jane Doe"
                />
                <FieldError message={errors.guestName} />
              </div>
              <div>
                <SimpleSelect id="rating" label="Rating" value={rating} onChange={setRating} allLabel="No rating" options={ratingOptions} />
                <FieldError message={errors.rating} />
              </div>
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="reviewText">Review</Label>
                <span className="text-xs text-muted-foreground">
                  {reviewText.length}/{REVIEW_TEXT_MAX}
                </span>
              </div>
              <Textarea
                id="reviewText"
                rows={7}
                value={reviewText}
                maxLength={REVIEW_TEXT_MAX}
                aria-invalid={!!errors.reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="How was the stay? Mention things like cleanliness, staff, breakfast, noise…"
              />
              <FieldError message={errors.reviewText} />
            </div>
            <div className="flex justify-end">
              <Button type="submit" size="lg" disabled={submit.isPending}>
                <SendIcon />
                {submit.isPending ? "Submitting…" : "Submit review"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      <div className="mt-6">
        <ImportReviewsCard />
      </div>
    </>
  );
}
