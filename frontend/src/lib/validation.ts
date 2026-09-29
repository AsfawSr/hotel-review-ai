import type { PolicyInput, ReviewSubmission } from "@/lib/types";

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

// Same constraints as PolicyRequest on the backend.
export const POLICY_LIMITS = { title: 200, category: 80, content: 8000, source: 200, tag: 60, tags: 20 } as const;

export function validatePolicy(input: PolicyInput): Record<string, string> {
  const errors: Record<string, string> = {};
  const required = (field: "title" | "category" | "content", label: string) => {
    const value = input[field].trim();
    if (!value) errors[field] = `${label} is required.`;
    else if (value.length > POLICY_LIMITS[field]) errors[field] = `${label} must be at most ${POLICY_LIMITS[field]} characters.`;
  };
  required("title", "Title");
  required("category", "Category");
  required("content", "Content");
  if (input.source && input.source.trim().length > POLICY_LIMITS.source) {
    errors.source = `Source must be at most ${POLICY_LIMITS.source} characters.`;
  }
  if (input.tags.length > POLICY_LIMITS.tags || input.tags.some((t) => !t.trim() || t.trim().length > POLICY_LIMITS.tag)) {
    errors.tags = `Up to ${POLICY_LIMITS.tags} non-empty tags of at most ${POLICY_LIMITS.tag} characters.`;
  }
  return errors;
}
