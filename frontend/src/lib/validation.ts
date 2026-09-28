import type { ReviewSubmission } from "@/lib/types";

// Same constraints as ReviewSubmissionRequest / Review entity on the backend.
export const GUEST_NAME_MAX = 100;
export const REVIEW_TEXT_MAX = 4000;

export function validateSubmission(input: ReviewSubmission): Record<string, string> {
  const errors: Record<string, string> = {};
  const name = input.guestName.trim();
  const text = input.reviewText.trim();

  if (!name) errors.guestName = "Guest name is required.";
  else if (name.length > GUEST_NAME_MAX) errors.guestName = `Guest name must be at most ${GUEST_NAME_MAX} characters.`;

  if (!text) errors.reviewText = "Review text is required.";
  else if (text.length > REVIEW_TEXT_MAX) errors.reviewText = `Review text must be at most ${REVIEW_TEXT_MAX} characters.`;

  if (input.rating != null && (!Number.isInteger(input.rating) || input.rating < 1 || input.rating > 5)) {
    errors.rating = "Rating must be between 1 and 5.";
  }
  return errors;
}
